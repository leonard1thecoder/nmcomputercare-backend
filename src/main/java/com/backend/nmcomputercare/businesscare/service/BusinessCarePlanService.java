package com.backend.nmcomputercare.businesscare.service;

import com.backend.nmcomputercare.basiccare.entity.DisplayStatus;
import com.backend.nmcomputercare.basiccare.entity.ComputerType;
import com.backend.nmcomputercare.businesscare.dtos.*;
import com.backend.nmcomputercare.businesscare.entity.BulkType;
import com.backend.nmcomputercare.businesscare.entity.BusinessCarePlan;
import com.backend.nmcomputercare.businesscare.entity.BusinessCareStatus;
import com.backend.nmcomputercare.businesscare.repository.BusinessCarePlanRepository;
import com.backend.nmcomputercare.businesscare.validator.BusinessCarePlanValidator;
import com.backend.nmcomputercare.performancecare.entity.CpuUpdate;
import com.backend.nmcomputercare.performancecare.entity.RamUpdate;
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
 * Service for all Business Care Plan operations.
 *
 * <h3>Pricing rules (summarised)</h3>
 * <ul>
 *   <li><b>PERFORMANCE_CARE</b>: upgradePrice (CPU→RAM→GPU priority, no summing)
 *       + deviceLabourPremium, then × quantity.</li>
 *   <li><b>BASIC_CARE</b>: OS + drivers + software + display (+ R100 laptop
 *       surcharge if applicable), then × quantity.</li>
 * </ul>
 *
 * <p>Rate limiters to configure in {@code application.yml}:
 * {@code businessCareCreate}, {@code businessCareUpdate}, {@code businessCareDelete}.
 */
@RequiredArgsConstructor
@Data
@Service
@Transactional
public class BusinessCarePlanService implements ExecuteService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessCarePlanService.class);

    private static final String REDIS_NS        = "BUSINESS_CARE:";
    private static final String REDIS_FIND_ALL  = REDIS_NS + "FIND_ALL";
    private static final String REDIS_BY_ID     = REDIS_NS + "BY_ID:";
    private static final String REDIS_BY_STATUS = REDIS_NS + "BY_STATUS:";

    private static final String BAD_REQUEST_MSG     = "Request contract type mismatch.";
    private static final String BAD_REQUEST_DETAILS =
            "Ensure the correct RequestContract subtype is passed for this service.";
    private static final String NOT_FOUND_DETAILS   =
            "No BusinessCarePlan record found for the given criteria.";

    @Value("${redis.setting.enable:false}")
    private boolean enableRedisUse;

    private final BusinessCarePlanRepository repository;
    private final RedisService               redisService;
    private final BusinessCarePlanValidator  validator;

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
                case "createBusinessCarePlan" ->
                        ((BusinessCarePlanService) AopContext.currentProxy()).createBusinessCarePlan(request);
                case "updateBusinessCarePlan" ->
                        ((BusinessCarePlanService) AopContext.currentProxy()).updateBusinessCarePlan(request);
                case "deleteBusinessCarePlan" ->
                        ((BusinessCarePlanService) AopContext.currentProxy()).deleteBusinessCarePlan(request);
                case "findAllBusinessCarePlans"       -> findAllBusinessCarePlans(toPageable(request));
                case "findBusinessCarePlanById"       -> findBusinessCarePlanById(request);
                case "findBusinessCarePlansByStatus"  -> findBusinessCarePlansByStatus(request);
                default -> {
                    ExceptionHandlerReporter.setResolveIssueDetails(
                            "Valid names: createBusinessCarePlan, updateBusinessCarePlan, " +
                            "deleteBusinessCarePlan, findAllBusinessCarePlans, " +
                            "findBusinessCarePlanById, findBusinessCarePlansByStatus.");
                    ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
                    throw new ServiceNameNotFoundException("Service name not found: " + serviceName);
                }
            };
        } finally {
            MDC.clear();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Write operations
    // ══════════════════════════════════════════════════════════════════════

    @RateLimiter(name = "businessCareCreate", fallbackMethod = "createFallback")
    public List<BusinessCarePlanResponse> createBusinessCarePlan(RequestContract request) {
        if (!(request instanceof CreateBusinessCarePlanRequest req)) throw badRequestException();
        validator.validateCreate(req);
        logger.info("Creating BusinessCarePlan | bulkType={} qty={}", req.getBulkType(), req.getQuantity());

        BusinessCarePlan entity = BusinessCarePlan.builder()
                .bulkType(req.getBulkType())
                .quantity(req.getQuantity())
                // Performance Care
                .cpuUpdate(req.getCpuUpdate())
                .intelModel(req.getIntelModel())
                .amdModel(req.getAmdModel())
                .ramUpdate(req.getRamUpdate())
                .ddr2Ram(req.getDdr2Ram())
                .ddr3Ram(req.getDdr3Ram())
                .ddr4Ram(req.getDdr4Ram())
                .deviceType(req.getDeviceType())
                .upgradeGraphicCard(req.getUpgradeGraphicCard())
                // Basic Care
                .operationSystem(req.getOperationSystem())
                .upgradeDrivers(req.getUpgradeDrivers())
                .additionalPerformanceSoftware(req.getAdditionalPerformanceSoftware())
                .issueDescription(req.getIssueDescription())
                .screenShotFilePath(req.getScreenShotFilePath())
                .displayStatus(req.getDisplayStatus())
                .computerType(req.getComputerType())
                .bookingDate(req.getBookingDate())
                .build();

        BusinessCarePlan saved = repository.save(entity);
        logger.info("BusinessCarePlan created | id={} totalQuote={}", saved.getId(), saved.getTotalQuote());
        invalidateListCaches();
        return toResponseList(List.of(saved));
    }

    @RateLimiter(name = "businessCareUpdate", fallbackMethod = "updateFallback")
    public List<BusinessCarePlanResponse> updateBusinessCarePlan(RequestContract request) {
        if (!(request instanceof UpdateBusinessCarePlanRequest req)) throw badRequestException();
        validator.validateUpdate(req);

        BusinessCarePlan plan = repository.findById(req.getId())
                .orElseThrow(() -> notFoundException("id=" + req.getId()));

        if (req.getBulkType()                     != null) plan.setBulkType(req.getBulkType());
        if (req.getQuantity()                     != null) plan.setQuantity(req.getQuantity());
        if (req.getCpuUpdate()                    != null) plan.setCpuUpdate(req.getCpuUpdate());
        if (req.getIntelModel()                   != null) plan.setIntelModel(req.getIntelModel());
        if (req.getAmdModel()                     != null) plan.setAmdModel(req.getAmdModel());
        if (req.getRamUpdate()                    != null) plan.setRamUpdate(req.getRamUpdate());
        if (req.getDdr2Ram()                      != null) plan.setDdr2Ram(req.getDdr2Ram());
        if (req.getDdr3Ram()                      != null) plan.setDdr3Ram(req.getDdr3Ram());
        if (req.getDdr4Ram()                      != null) plan.setDdr4Ram(req.getDdr4Ram());
        if (req.getDeviceType()                   != null) plan.setDeviceType(req.getDeviceType());
        if (req.getUpgradeGraphicCard()            != null) plan.setUpgradeGraphicCard(req.getUpgradeGraphicCard());
        if (req.getOperationSystem()              != null) plan.setOperationSystem(req.getOperationSystem());
        if (req.getUpgradeDrivers()               != null) plan.setUpgradeDrivers(req.getUpgradeDrivers());
        if (req.getAdditionalPerformanceSoftware() != null) plan.setAdditionalPerformanceSoftware(req.getAdditionalPerformanceSoftware());
        if (req.getIssueDescription()             != null) plan.setIssueDescription(req.getIssueDescription());
        if (req.getScreenShotFilePath()           != null) plan.setScreenShotFilePath(req.getScreenShotFilePath());
        if (req.getDisplayStatus()                != null) plan.setDisplayStatus(req.getDisplayStatus());
        if (req.getComputerType()                 != null) plan.setComputerType(req.getComputerType());
        if (req.getBookingDate()                  != null) plan.setBookingDate(req.getBookingDate());
        if (req.getStatus()                       != null) plan.setStatus(req.getStatus());

        BusinessCarePlan saved = repository.save(plan);
        evictCache(REDIS_BY_ID + saved.getId());
        evictCache(REDIS_BY_STATUS + saved.getStatus());
        invalidateListCaches();
        logger.info("BusinessCarePlan updated | id={} totalQuote={}", saved.getId(), saved.getTotalQuote());
        return toResponseList(List.of(saved));
    }

    @RateLimiter(name = "businessCareDelete", fallbackMethod = "deleteFallback")
    public List<BusinessCarePlanResponse> deleteBusinessCarePlan(RequestContract request) {
        if (!(request instanceof DeleteBusinessCarePlanRequest req)) throw badRequestException();
        if (req.getId() == null || req.getId() <= 0) {
            ExceptionHandlerReporter.setResolveIssueDetails("Provide a valid positive id.");
            ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
            throw new IncorrectRequestSentException("Invalid id: " + req.getId());
        }
        BusinessCarePlan plan = repository.findById(req.getId())
                .orElseThrow(() -> notFoundException("id=" + req.getId()));
        List<BusinessCarePlanResponse> snapshot = toResponseList(List.of(plan));
        repository.delete(plan);
        evictCache(REDIS_BY_ID + plan.getId());
        evictCache(REDIS_BY_STATUS + plan.getStatus());
        invalidateListCaches();
        logger.info("BusinessCarePlan deleted | id={}", plan.getId());
        return snapshot;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Read operations
    // ══════════════════════════════════════════════════════════════════════

    private List<BusinessCarePlanResponse> findAllBusinessCarePlans(Pageable pageable) {
        boolean cacheable = pageable.getPageNumber() == 0 && enableRedisUse;
        if (cacheable) {
            List<BusinessCarePlanResponse> cached =
                    redisService.getList(REDIS_FIND_ALL, BusinessCarePlanResponse.class);
            if (cached != null && !cached.isEmpty()) return cached;
        }
        Page<BusinessCarePlan> page = repository.findAll(pageable);
        List<BusinessCarePlanResponse> result = toResponseList(page.getContent());
        if (cacheable && !result.isEmpty())
            redisService.setList(REDIS_FIND_ALL, result, 12L, TimeUnit.HOURS);
        return result;
    }

    private List<BusinessCarePlanResponse> findBusinessCarePlanById(RequestContract request) {
        if (!(request instanceof FindBusinessCarePlanByIdRequest req)) throw badRequestException();
        String key = REDIS_BY_ID + req.getId();
        if (enableRedisUse) {
            List<BusinessCarePlanResponse> cached = redisService.getList(key, BusinessCarePlanResponse.class);
            if (cached != null && !cached.isEmpty()) return cached;
        }
        BusinessCarePlan found = repository.findById(req.getId())
                .orElseThrow(() -> notFoundException("id=" + req.getId()));
        List<BusinessCarePlanResponse> result = toResponseList(List.of(found));
        if (enableRedisUse) redisService.setList(key, result, 12L, TimeUnit.HOURS);
        return result;
    }

    private List<BusinessCarePlanResponse> findBusinessCarePlansByStatus(RequestContract request) {
        if (!(request instanceof FindBusinessCarePlansByStatusRequest req)) throw badRequestException();
        BusinessCareStatus.fromCode(req.getStatus());
        Pageable pageable = req.toPageable();
        String   key      = REDIS_BY_STATUS + req.getStatus();
        boolean  cacheable = pageable.getPageNumber() == 0 && enableRedisUse;
        if (cacheable) {
            List<BusinessCarePlanResponse> cached = redisService.getList(key, BusinessCarePlanResponse.class);
            if (cached != null && !cached.isEmpty()) return cached;
        }
        Page<BusinessCarePlan> page = repository.findByStatus(req.getStatus(), pageable);
        List<BusinessCarePlanResponse> result = toResponseList(page.getContent());
        if (cacheable && !result.isEmpty()) redisService.setList(key, result, 1L, TimeUnit.HOURS);
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Rate-limiter fallbacks
    // ══════════════════════════════════════════════════════════════════════

    public List<BusinessCarePlanResponse> createFallback(RequestContract r, RequestNotPermitted ex) {
        ExceptionHandlerReporter.setResolveIssueDetails("Too many create requests. Please wait.");
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        throw new IncorrectRequestSentException("Too many create requests.");
    }
    public List<BusinessCarePlanResponse> updateFallback(RequestContract r, RequestNotPermitted ex) {
        ExceptionHandlerReporter.setResolveIssueDetails("Too many update requests. Please wait.");
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        throw new IncorrectRequestSentException("Too many update requests.");
    }
    public List<BusinessCarePlanResponse> deleteFallback(RequestContract r, RequestNotPermitted ex) {
        ExceptionHandlerReporter.setResolveIssueDetails("Too many delete requests. Please wait.");
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        throw new IncorrectRequestSentException("Too many delete requests.");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Response mapping
    // ══════════════════════════════════════════════════════════════════════

    private List<BusinessCarePlanResponse> toResponseList(List<BusinessCarePlan> plans) {
        return plans.stream().map(p -> {
            BusinessCareStatus statusEnum = BusinessCareStatus.fromCode(p.getStatus());

            // ── Resolve performance prices ────────────────────────────────
            BigDecimal cpuPrice = resolveCpuPrice(p);
            BigDecimal ramPrice = resolveRamPrice(p);
            BigDecimal gpuPrice = (p.getUpgradeGraphicCard() != null)
                    ? p.getUpgradeGraphicCard().getPrice() : BigDecimal.ZERO;

            BigDecimal appliedUpgradePrice = BigDecimal.ZERO;
            if (p.getBulkType() == BulkType.PERFORMANCE_CARE) {
                if (cpuPrice.compareTo(BigDecimal.ZERO) > 0)      appliedUpgradePrice = cpuPrice;
                else if (ramPrice.compareTo(BigDecimal.ZERO) > 0) appliedUpgradePrice = ramPrice;
                else                                               appliedUpgradePrice = gpuPrice;
            }

            // ── Resolve basic care prices ─────────────────────────────────
            BigDecimal laptopSurcharge = (p.getDisplayStatus()  == DisplayStatus.NO
                                       && p.getComputerType()   == ComputerType.LAPTOP)
                    ? new BigDecimal("100.00") : BigDecimal.ZERO;

            // ── unit price for breakdown display ──────────────────────────
            BigDecimal devicePremium = (p.getDeviceType() != null)
                    ? p.getDeviceType().getLabourPremium() : BigDecimal.ZERO;
            BigDecimal unitPrice = (p.getBulkType() == BulkType.PERFORMANCE_CARE)
                    ? appliedUpgradePrice.add(devicePremium)
                    : computeBasicUnitPrice(p).add(laptopSurcharge);

            return BusinessCarePlanResponse.builder()
                    .id(p.getId())
                    // Bulk metadata
                    .bulkType(p.getBulkType())
                    .bulkTypeDisplayName(p.getBulkType() != null ? p.getBulkType().getDisplayName() : null)
                    .quantity(p.getQuantity())
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
                    .deviceLabourPremium(devicePremium)
                    .upgradeGraphicCard(p.getUpgradeGraphicCard())
                    .upgradeGraphicCardDisplayName(p.getUpgradeGraphicCard() != null
                            ? p.getUpgradeGraphicCard().getDisplayName() : null)
                    .upgradeGraphicCardPrice(p.getUpgradeGraphicCard() != null
                            ? p.getUpgradeGraphicCard().getPrice() : null)
                    .appliedUpgradePrice(appliedUpgradePrice)
                    // Basic Care
                    .operationSystem(p.getOperationSystem())
                    .operationSystemDisplayName(p.getOperationSystem() != null
                            ? p.getOperationSystem().getDisplayName() : null)
                    .operationSystemPrice(p.getOperationSystem() != null
                            ? p.getOperationSystem().getPrice() : null)
                    .upgradeDrivers(p.getUpgradeDrivers())
                    .upgradeDriversDisplayName(p.getUpgradeDrivers() != null
                            ? p.getUpgradeDrivers().getDisplayName() : null)
                    .upgradeDriversPrice(p.getUpgradeDrivers() != null
                            ? p.getUpgradeDrivers().getPrice() : null)
                    .additionalPerformanceSoftware(p.getAdditionalPerformanceSoftware())
                    .additionalPerformanceSoftwareDisplayName(p.getAdditionalPerformanceSoftware() != null
                            ? p.getAdditionalPerformanceSoftware().getDisplayName() : null)
                    .additionalPerformanceSoftwarePrice(p.getAdditionalPerformanceSoftware() != null
                            ? p.getAdditionalPerformanceSoftware().getPrice() : null)
                    .issueDescription(p.getIssueDescription())
                    .screenShotFilePath(p.getScreenShotFilePath())
                    .displayStatus(p.getDisplayStatus())
                    .displayStatusDisplayName(p.getDisplayStatus() != null
                            ? p.getDisplayStatus().getDisplayName() : null)
                    .displayStatusPrice(p.getDisplayStatus() != null
                            ? p.getDisplayStatus().getPrice() : null)
                    .computerType(p.getComputerType())
                    .computerTypeDisplayName(p.getComputerType() != null
                            ? p.getComputerType().getDisplayName() : null)
                    .laptopDisplaySurcharge(laptopSurcharge)
                    // Quote
                    .unitPrice(unitPrice)
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

    // ── Price resolvers ────────────────────────────────────────────────────

    private BigDecimal resolveCpuPrice(BusinessCarePlan p) {
        if (p.getCpuUpdate() == null || p.getCpuUpdate() == CpuUpdate.NONE) return BigDecimal.ZERO;
        if (p.getCpuUpdate() == CpuUpdate.INTEL && p.getIntelModel() != null) return p.getIntelModel().getPrice();
        if (p.getCpuUpdate() == CpuUpdate.AMD   && p.getAmdModel()   != null) return p.getAmdModel().getPrice();
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveRamPrice(BusinessCarePlan p) {
        if (p.getRamUpdate() == null || p.getRamUpdate() == RamUpdate.NONE) return BigDecimal.ZERO;
        return switch (p.getRamUpdate()) {
            case DDR2 -> (p.getDdr2Ram() != null ? p.getDdr2Ram().getPrice() : BigDecimal.ZERO);
            case DDR3 -> (p.getDdr3Ram() != null ? p.getDdr3Ram().getPrice() : BigDecimal.ZERO);
            case DDR4 -> (p.getDdr4Ram() != null ? p.getDdr4Ram().getPrice() : BigDecimal.ZERO);
            default   -> BigDecimal.ZERO;
        };
    }

    private BigDecimal computeBasicUnitPrice(BusinessCarePlan p) {
        BigDecimal os  = p.getOperationSystem()               != null ? p.getOperationSystem().getPrice()               : BigDecimal.ZERO;
        BigDecimal drv = p.getUpgradeDrivers()                != null ? p.getUpgradeDrivers().getPrice()                : BigDecimal.ZERO;
        BigDecimal sfw = p.getAdditionalPerformanceSoftware() != null ? p.getAdditionalPerformanceSoftware().getPrice() : BigDecimal.ZERO;
        BigDecimal dsp = p.getDisplayStatus()                 != null ? p.getDisplayStatus().getPrice()                 : BigDecimal.ZERO;
        return os.add(drv).add(sfw).add(dsp);
    }

    // ── Cache helpers ──────────────────────────────────────────────────────

    private void invalidateListCaches() { evictCache(REDIS_FIND_ALL); }

    private void evictCache(String key) {
        if (enableRedisUse) {
            redisService.delete(key);
            logger.debug("Cache evicted | key={}", key);
        }
    }

    private Pageable toPageable(RequestContract r) {
        if (r instanceof PageableRequest pr) return pr.toPageable();
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
        return new IncorrectRequestSentException("BusinessCarePlan not found: " + criteria);
    }
}
