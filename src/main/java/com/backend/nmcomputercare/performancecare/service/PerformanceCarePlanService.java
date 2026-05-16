package com.backend.nmcomputercare.performancecare.service;

import com.backend.nmcomputercare.performancecare.dtos.*;
import com.backend.nmcomputercare.performancecare.entity.*;
import com.backend.nmcomputercare.performancecare.repository.PerformanceCarePlanRepository;
import com.backend.nmcomputercare.performancecare.validator.PerformanceCarePlanValidator;
import com.backend.nmcomputercare.utils.*;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import com.backend.nmcomputercare.utils.exceptions.ServiceNameNotFoundException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service handling all Performance Care Plan operations.
 *
 * <h3>Key pricing rule</h3>
 * CPU, RAM and GPU prices are <em>not</em> summed.  Only the highest-priority
 * selected upgrade price is charged (CPU → RAM → GPU).  The device labour
 * premium is always added on top.  See
 * {@link PerformanceCarePlan#computeQuote()} for the implementation.
 *
 * <p>Configure these Resilience4j rate limiters in {@code application.yml}:
 * {@code performanceCareCreate}, {@code performanceCareUpdate},
 * {@code performanceCareDelete}.
 */
@RequiredArgsConstructor
@Data
@Service
@Transactional
public class PerformanceCarePlanService implements ExecuteService {

    private static final Logger logger =
            LoggerFactory.getLogger(PerformanceCarePlanService.class);

    // ── Redis keys ─────────────────────────────────────────────────────────
    private static final String REDIS_NS        = "PERFORMANCE_CARE:";
    private static final String REDIS_FIND_ALL  = REDIS_NS + "FIND_ALL";
    private static final String REDIS_BY_ID     = REDIS_NS + "BY_ID:";
    private static final String REDIS_BY_STATUS = REDIS_NS + "BY_STATUS:";

    // ── Shared messages ────────────────────────────────────────────────────
    private static final String BAD_REQUEST_MSG     = "Request contract type mismatch.";
    private static final String BAD_REQUEST_DETAILS =
            "Ensure the correct RequestContract subtype is passed for this service.";
    private static final String NOT_FOUND_DETAILS =
            "No PerformanceCarePlan record found for the given criteria.";

    @Value("${redis.setting.enable:false}")
    private boolean enableRedisUse;

    private final PerformanceCarePlanRepository  repository;
    private final RedisService                   redisService;
    private final PerformanceCarePlanValidator   validator;

    // ══════════════════════════════════════════════════════════════════════
    //  Dispatcher
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public List<? extends ResponseContract> callable(String serviceName, RequestContract request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("serviceName",   serviceName);

        try {
            logger.info("Service invoked | service={} correlationId={}", serviceName, correlationId);

            return switch (serviceName) {
                case "createPerformanceCarePlan" ->
                        ((PerformanceCarePlanService) AopContext.currentProxy()).createPerformanceCarePlan(request);
                case "updatePerformanceCarePlan" ->
                        ((PerformanceCarePlanService) AopContext.currentProxy()).updatePerformanceCarePlan(request);
                case "deletePerformanceCarePlan" ->
                        ((PerformanceCarePlanService) AopContext.currentProxy()).deletePerformanceCarePlan(request);

                case "findAllPerformanceCarePlans"      -> findAllPerformanceCarePlans(toPageable(request));
                case "findPerformanceCarePlanById"      -> findPerformanceCarePlanById(request);
                case "findPerformanceCarePlansByStatus" -> findPerformanceCarePlansByStatus(request);

                default -> {
                    logger.warn("Unknown service | service={} correlationId={}",
                            serviceName, correlationId);
                    ExceptionHandlerReporter.setResolveIssueDetails(
                            "Valid names: createPerformanceCarePlan, updatePerformanceCarePlan, " +
                            "deletePerformanceCarePlan, findAllPerformanceCarePlans, " +
                            "findPerformanceCarePlanById, findPerformanceCarePlansByStatus.");
                    ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
                    throw new ServiceNameNotFoundException("Service name not found: " + serviceName);
                }
            };
        } finally {
            logger.debug("Dispatch complete | service={} correlationId={}", serviceName, correlationId);
            MDC.clear();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Write operations
    // ══════════════════════════════════════════════════════════════════════

    @RateLimiter(name = "performanceCareCreate", fallbackMethod = "createFallback")
    public List<PerformanceCarePlanResponse> createPerformanceCarePlan(RequestContract request) {
        if (!(request instanceof CreatePerformanceCarePlanRequest req)) throw badRequestException();

        validator.validateCreate(req);
        logger.info("Creating PerformanceCarePlan | cpu={} ram={} gpu={} device={}",
                req.getCpuUpdate(), req.getRamUpdate(),
                req.getUpgradeGraphicCard(), req.getDeviceType());

        PerformanceCarePlan entity = PerformanceCarePlan.builder()
                .cpuUpdate(req.getCpuUpdate())
                .intelModel(req.getIntelModel())
                .amdModel(req.getAmdModel())
                .ramUpdate(req.getRamUpdate())
                .ddr2Ram(req.getDdr2Ram())
                .ddr3Ram(req.getDdr3Ram())
                .ddr4Ram(req.getDdr4Ram())
                .deviceType(req.getDeviceType())
                .upgradeGraphicCard(req.getUpgradeGraphicCard())
                .bookingDate(req.getBookingDate())
                .build();

        PerformanceCarePlan saved = repository.save(entity);
        logger.info("PerformanceCarePlan created | id={} totalQuote={}",
                saved.getId(), saved.getTotalQuote());

        invalidateListCaches();
        return toResponseList(List.of(saved));
    }

    @RateLimiter(name = "performanceCareUpdate", fallbackMethod = "updateFallback")
    public List<PerformanceCarePlanResponse> updatePerformanceCarePlan(RequestContract request) {
        if (!(request instanceof UpdatePerformanceCarePlanRequest req)) throw badRequestException();

        validator.validateUpdate(req);
        logger.info("Updating PerformanceCarePlan | id={}", req.getId());

        PerformanceCarePlan plan = repository.findById(req.getId())
                .orElseThrow(() -> notFoundException("id=" + req.getId()));

        // Partial update — apply only non-null fields.
        if (req.getCpuUpdate()          != null) plan.setCpuUpdate(req.getCpuUpdate());
        if (req.getIntelModel()         != null) plan.setIntelModel(req.getIntelModel());
        if (req.getAmdModel()           != null) plan.setAmdModel(req.getAmdModel());
        if (req.getRamUpdate()          != null) plan.setRamUpdate(req.getRamUpdate());
        if (req.getDdr2Ram()            != null) plan.setDdr2Ram(req.getDdr2Ram());
        if (req.getDdr3Ram()            != null) plan.setDdr3Ram(req.getDdr3Ram());
        if (req.getDdr4Ram()            != null) plan.setDdr4Ram(req.getDdr4Ram());
        if (req.getDeviceType()         != null) plan.setDeviceType(req.getDeviceType());
        if (req.getUpgradeGraphicCard() != null) plan.setUpgradeGraphicCard(req.getUpgradeGraphicCard());
        if (req.getBookingDate()        != null) plan.setBookingDate(req.getBookingDate());
        if (req.getStatus()             != null) plan.setStatus(req.getStatus());

        PerformanceCarePlan saved = repository.save(plan);
        logger.info("PerformanceCarePlan updated | id={} status={} totalQuote={}",
                saved.getId(), saved.getStatus(), saved.getTotalQuote());

        evictCache(REDIS_BY_ID     + saved.getId());
        evictCache(REDIS_BY_STATUS + saved.getStatus());
        invalidateListCaches();
        return toResponseList(List.of(saved));
    }

    @RateLimiter(name = "performanceCareDelete", fallbackMethod = "deleteFallback")
    public List<PerformanceCarePlanResponse> deletePerformanceCarePlan(RequestContract request) {
        if (!(request instanceof DeletePerformanceCarePlanRequest req)) throw badRequestException();

        if (req.getId() == null || req.getId() <= 0) {
            ExceptionHandlerReporter.setResolveIssueDetails("Provide a valid positive id.");
            ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
            throw new IncorrectRequestSentException("Invalid id for delete: " + req.getId());
        }

        PerformanceCarePlan plan = repository.findById(req.getId())
                .orElseThrow(() -> notFoundException("id=" + req.getId()));

        List<PerformanceCarePlanResponse> snapshot = toResponseList(List.of(plan));
        repository.delete(plan);

        evictCache(REDIS_BY_ID     + plan.getId());
        evictCache(REDIS_BY_STATUS + plan.getStatus());
        invalidateListCaches();
        logger.info("PerformanceCarePlan deleted | id={}", plan.getId());
        return snapshot;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Read operations
    // ══════════════════════════════════════════════════════════════════════

    private List<PerformanceCarePlanResponse> findAllPerformanceCarePlans(Pageable pageable) {
        boolean isCacheable = pageable.getPageNumber() == 0 && enableRedisUse;
        if (isCacheable) {
            List<PerformanceCarePlanResponse> cached =
                    redisService.getList(REDIS_FIND_ALL, PerformanceCarePlanResponse.class);
            if (cached != null && !cached.isEmpty()) return cached;
        }
        Page<PerformanceCarePlan> page = repository.findAll(pageable);
        List<PerformanceCarePlanResponse> result = toResponseList(page.getContent());
        if (isCacheable && !result.isEmpty())
            redisService.setList(REDIS_FIND_ALL, result, 12L, TimeUnit.HOURS);
        logger.info("findAll complete | total={}", page.getTotalElements());
        return result;
    }

    private List<PerformanceCarePlanResponse> findPerformanceCarePlanById(RequestContract request) {
        if (!(request instanceof FindPerformanceCarePlanByIdRequest req)) throw badRequestException();
        String redisKey = REDIS_BY_ID + req.getId();
        if (enableRedisUse) {
            List<PerformanceCarePlanResponse> cached =
                    redisService.getList(redisKey, PerformanceCarePlanResponse.class);
            if (cached != null && !cached.isEmpty()) return cached;
        }
        PerformanceCarePlan found = repository.findById(req.getId())
                .orElseThrow(() -> notFoundException("id=" + req.getId()));
        List<PerformanceCarePlanResponse> result = toResponseList(List.of(found));
        if (enableRedisUse && !result.isEmpty())
            redisService.setList(redisKey, result, 12L, TimeUnit.HOURS);
        return result;
    }

    private List<PerformanceCarePlanResponse> findPerformanceCarePlansByStatus(RequestContract request) {
        if (!(request instanceof FindPerformanceCarePlansByStatusRequest req)) throw badRequestException();
        PerformanceCareStatus.fromCode(req.getStatus()); // validate code
        Pageable pageable    = req.toPageable();
        String   redisKey    = REDIS_BY_STATUS + req.getStatus();
        boolean  isCacheable = pageable.getPageNumber() == 0 && enableRedisUse;
        if (isCacheable) {
            List<PerformanceCarePlanResponse> cached =
                    redisService.getList(redisKey, PerformanceCarePlanResponse.class);
            if (cached != null && !cached.isEmpty()) return cached;
        }
        Page<PerformanceCarePlan> page =
                repository.findByStatus(req.getStatus(), pageable);
        List<PerformanceCarePlanResponse> result = toResponseList(page.getContent());
        if (isCacheable && !result.isEmpty())
            redisService.setList(redisKey, result, 1L, TimeUnit.HOURS);
        logger.info("findByStatus complete | status={} count={}", req.getStatus(), result.size());
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Rate-limiter fallbacks
    // ══════════════════════════════════════════════════════════════════════

    public List<PerformanceCarePlanResponse> createFallback(
            RequestContract r, RequestNotPermitted ex) {
        logger.warn("Rate limit | limiter=performanceCareCreate");
        ExceptionHandlerReporter.setResolveIssueDetails("Too many create requests. Please wait.");
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        throw new IncorrectRequestSentException("Too many create requests. Please slow down.");
    }

    public List<PerformanceCarePlanResponse> updateFallback(
            RequestContract r, RequestNotPermitted ex) {
        logger.warn("Rate limit | limiter=performanceCareUpdate");
        ExceptionHandlerReporter.setResolveIssueDetails("Too many update requests. Please wait.");
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        throw new IncorrectRequestSentException("Too many update requests. Please slow down.");
    }

    public List<PerformanceCarePlanResponse> deleteFallback(
            RequestContract r, RequestNotPermitted ex) {
        logger.warn("Rate limit | limiter=performanceCareDelete");
        ExceptionHandlerReporter.setResolveIssueDetails("Too many delete requests. Please wait.");
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        throw new IncorrectRequestSentException("Too many delete requests. Please slow down.");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════

    private List<PerformanceCarePlanResponse> toResponseList(List<PerformanceCarePlan> plans) {
        return plans.stream().map(p -> {
            PerformanceCareStatus statusEnum = PerformanceCareStatus.fromCode(p.getStatus());

            // Resolve per-component prices for the response breakdown.
            BigDecimal cpuPrice = resolveCpuPrice(p);
            BigDecimal ramPrice = resolveRamPrice(p);
            BigDecimal gpuPrice = p.getUpgradeGraphicCard() != null
                    ? p.getUpgradeGraphicCard().getPrice() : BigDecimal.ZERO;

            BigDecimal appliedUpgradePrice;
            if (cpuPrice.compareTo(BigDecimal.ZERO) > 0)       appliedUpgradePrice = cpuPrice;
            else if (ramPrice.compareTo(BigDecimal.ZERO) > 0)  appliedUpgradePrice = ramPrice;
            else                                                 appliedUpgradePrice = gpuPrice;

            return PerformanceCarePlanResponse.builder()
                    .id(p.getId())
                    // CPU
                    .cpuUpdate(p.getCpuUpdate())
                    .intelModel(p.getIntelModel())
                    .intelModelDisplayName(p.getIntelModel() != null ? p.getIntelModel().getDisplayName() : null)
                    .intelModelPrice(p.getIntelModel() != null ? p.getIntelModel().getPrice() : null)
                    .amdModel(p.getAmdModel())
                    .amdModelDisplayName(p.getAmdModel() != null ? p.getAmdModel().getDisplayName() : null)
                    .amdModelPrice(p.getAmdModel() != null ? p.getAmdModel().getPrice() : null)
                    .resolvedCpuPrice(cpuPrice)
                    // RAM
                    .ramUpdate(p.getRamUpdate())
                    .ddr2Ram(p.getDdr2Ram())
                    .ddr2RamDisplayName(p.getDdr2Ram() != null ? p.getDdr2Ram().getDisplayName() : null)
                    .ddr2RamPrice(p.getDdr2Ram() != null ? p.getDdr2Ram().getPrice() : null)
                    .ddr3Ram(p.getDdr3Ram())
                    .ddr3RamDisplayName(p.getDdr3Ram() != null ? p.getDdr3Ram().getDisplayName() : null)
                    .ddr3RamPrice(p.getDdr3Ram() != null ? p.getDdr3Ram().getPrice() : null)
                    .ddr4Ram(p.getDdr4Ram())
                    .ddr4RamDisplayName(p.getDdr4Ram() != null ? p.getDdr4Ram().getDisplayName() : null)
                    .ddr4RamPrice(p.getDdr4Ram() != null ? p.getDdr4Ram().getPrice() : null)
                    .resolvedRamPrice(ramPrice)
                    // Device & GPU
                    .deviceType(p.getDeviceType())
                    .deviceTypeDisplayName(p.getDeviceType() != null ? p.getDeviceType().getDisplayName() : null)
                    .deviceLabourPremium(p.getDeviceType() != null ? p.getDeviceType().getLabourPremium() : null)
                    .upgradeGraphicCard(p.getUpgradeGraphicCard())
                    .upgradeGraphicCardDisplayName(p.getUpgradeGraphicCard() != null
                            ? p.getUpgradeGraphicCard().getDisplayName() : null)
                    .upgradeGraphicCardPrice(p.getUpgradeGraphicCard() != null
                            ? p.getUpgradeGraphicCard().getPrice() : null)
                    // Quote breakdown
                    .appliedUpgradePrice(appliedUpgradePrice)
                    .totalQuote(p.getTotalQuote())
                    // Status
                    .status(p.getStatus())
                    .statusEnum(statusEnum)
                    .statusDisplayName(statusEnum.getDisplayName())
                    // Timestamps
                    .createdDate(p.getCreatedDate())
                    .modifiedDate(p.getModifiedDate())
                    .bookingDate(p.getBookingDate())
                    .build();
        }).toList();
    }

    private BigDecimal resolveCpuPrice(PerformanceCarePlan p) {
        if (p.getCpuUpdate() == null || p.getCpuUpdate() == CpuUpdate.NONE) return BigDecimal.ZERO;
        if (p.getCpuUpdate() == CpuUpdate.INTEL && p.getIntelModel() != null)
            return p.getIntelModel().getPrice();
        if (p.getCpuUpdate() == CpuUpdate.AMD && p.getAmdModel() != null)
            return p.getAmdModel().getPrice();
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveRamPrice(PerformanceCarePlan p) {
        if (p.getRamUpdate() == null || p.getRamUpdate() == RamUpdate.NONE) return BigDecimal.ZERO;
        return switch (p.getRamUpdate()) {
            case DDR2 -> (p.getDdr2Ram() != null ? p.getDdr2Ram().getPrice() : BigDecimal.ZERO);
            case DDR3 -> (p.getDdr3Ram() != null ? p.getDdr3Ram().getPrice() : BigDecimal.ZERO);
            case DDR4 -> (p.getDdr4Ram() != null ? p.getDdr4Ram().getPrice() : BigDecimal.ZERO);
            default   -> BigDecimal.ZERO;
        };
    }

    private void invalidateListCaches() {
        evictCache(REDIS_FIND_ALL);
    }

    private void evictCache(String key) {
        if (enableRedisUse) {
            redisService.delete(key);
            logger.debug("Cache evicted | key={}", key);
        }
    }

    private Pageable toPageable(RequestContract request) {
        if (request instanceof PageableRequest pr) return pr.toPageable();
        return org.springframework.data.domain.PageRequest.of(0, 20);
    }

    private RuntimeException badRequestException() {
        ExceptionHandlerReporter.setResolveIssueDetails(BAD_REQUEST_DETAILS);
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        return new IncorrectRequestSentException(BAD_REQUEST_MSG);
    }

    private RuntimeException notFoundException(String criteria) {
        ExceptionHandlerReporter.setResolveIssueDetails(NOT_FOUND_DETAILS);
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        return new IncorrectRequestSentException("PerformanceCarePlan not found: " + criteria);
    }
}
