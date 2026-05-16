package com.backend.nmcomputercare.basiccare.service;

import com.backend.nmcomputercare.basiccare.dtos.*;
import com.backend.nmcomputercare.basiccare.entity.BasicCarePlan;
import com.backend.nmcomputercare.basiccare.entity.BasicCareStatus;
import com.backend.nmcomputercare.basiccare.repository.BasicCarePlanRepository;
import com.backend.nmcomputercare.basiccare.validator.BasicCarePlanValidator;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service handling all Basic Care Plan operations: create, update, delete,
 * paginated retrieval, status filtering, Redis caching, and Resilience4j
 * rate limiting for write operations.
 *
 * <p>Configure the following rate limiters in {@code application.yml}:
 * <ul>
 *   <li>{@code "basicCareCreate"}</li>
 *   <li>{@code "basicCareUpdate"}</li>
 *   <li>{@code "basicCareDelete"}</li>
 * </ul>
 */
@RequiredArgsConstructor
@Data
@Service
@Transactional
public class BasicCarePlanService implements ExecuteService {

    private static final Logger logger = LoggerFactory.getLogger(BasicCarePlanService.class);

    // ── Redis key prefixes ──────────────────────────────────────────────────
    private static final String REDIS_NS         = "BASIC_CARE:";
    private static final String REDIS_FIND_ALL   = REDIS_NS + "FIND_ALL";
    private static final String REDIS_BY_ID      = REDIS_NS + "BY_ID:";
    private static final String REDIS_BY_STATUS  = REDIS_NS + "BY_STATUS:";

    // ── Shared messages ─────────────────────────────────────────────────────
    private static final String BAD_REQUEST_MSG     = "Request contract type mismatch.";
    private static final String BAD_REQUEST_DETAILS = "Ensure the correct RequestContract subtype is passed for this service.";
    private static final String NOT_FOUND_DETAILS   = "No BasicCarePlan record found for the given criteria.";

    @Value("${redis.setting.enable:false}")
    private boolean enableRedisUse;

    private final BasicCarePlanRepository repository;
    private final RedisService            redisService;
    private final BasicCarePlanValidator  validator;

    // ══════════════════════════════════════════════════════════════════════
    //  Dispatcher
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public List<? extends ResponseContract> callable(String serviceName, RequestContract request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("serviceName", serviceName);

        try {
            logger.info("Service invoked | service={} correlationId={}", serviceName, correlationId);

            return switch (serviceName) {
                // Route writes through the self-proxy so @RateLimiter fires.
                case "createBasicCarePlan" ->
                        ((BasicCarePlanService) AopContext.currentProxy()).createBasicCarePlan(request);
                case "updateBasicCarePlan" ->
                        ((BasicCarePlanService) AopContext.currentProxy()).updateBasicCarePlan(request);
                case "deleteBasicCarePlan" ->
                        ((BasicCarePlanService) AopContext.currentProxy()).deleteBasicCarePlan(request);

                case "findAllBasicCarePlans"    -> findAllBasicCarePlans(toPageable(request));
                case "findBasicCarePlanById"    -> findBasicCarePlanById(request);
                case "findBasicCarePlansByStatus" -> findBasicCarePlansByStatus(request);

                default -> {
                    String msg = "Service name not found: " + serviceName;
                    logger.warn("Unknown service requested | service={} correlationId={}", serviceName, correlationId);
                    ExceptionHandlerReporter.setResolveIssueDetails(
                            "Ensure the service name matches one of: createBasicCarePlan, updateBasicCarePlan, " +
                            "deleteBasicCarePlan, findAllBasicCarePlans, findBasicCarePlanById, findBasicCarePlansByStatus.");
                    ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
                    throw new ServiceNameNotFoundException(msg);
                }
            };
        } finally {
            logger.debug("Service dispatch complete | service={} correlationId={}", serviceName, correlationId);
            MDC.clear();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Write operations
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Creates a new Basic Care Plan.
     *
     * <ul>
     *   <li>Validates all required fields before persisting.</li>
     *   <li>Total quote is computed automatically by the entity's {@code @PrePersist} hook.</li>
     *   <li>Initial status is always {@code CREATED (0)}.</li>
     * </ul>
     */
    @RateLimiter(name = "basicCareCreate", fallbackMethod = "createFallback")
    public List<BasicCarePlanResponse> createBasicCarePlan(RequestContract request) {
        if (!(request instanceof CreateBasicCarePlanRequest castedRequest)) {
            throw badRequestException();
        }

        validator.validateCreate(castedRequest);
        logger.info("Creating BasicCarePlan | os={} drivers={} software={}",
                castedRequest.getOperationSystem(),
                castedRequest.getUpgradeDrivers(),
                castedRequest.getAdditionalPerformanceSoftware());

        BasicCarePlan entity = BasicCarePlan.builder()
                .operationSystem(castedRequest.getOperationSystem())
                .upgradeDrivers(castedRequest.getUpgradeDrivers())
                .additionalPerformanceSoftware(castedRequest.getAdditionalPerformanceSoftware())
                .issueDescription(castedRequest.getIssueDescription())
                .screenShotFilePath(castedRequest.getScreenShotFilePath())
                .build();

        BasicCarePlan saved = repository.save(entity);
        logger.info("BasicCarePlan created | id={} totalQuote={}", saved.getId(), saved.getTotalQuote());

        invalidateListCaches();
        return toResponseList(List.of(saved));
    }

    /**
     * Partially updates an existing Basic Care Plan.
     *
     * <p>Only non-null fields in the request are applied; all others are left
     * unchanged.  The quote is recomputed after any component change via the
     * entity's {@code @PreUpdate} hook.
     */
    @RateLimiter(name = "basicCareUpdate", fallbackMethod = "updateFallback")
    public List<BasicCarePlanResponse> updateBasicCarePlan(RequestContract request) {
        if (!(request instanceof UpdateBasicCarePlanRequest castedRequest)) {
            throw badRequestException();
        }

        validator.validateUpdate(castedRequest);
        logger.info("Updating BasicCarePlan | id={}", castedRequest.getId());

        BasicCarePlan plan = repository.findById(castedRequest.getId())
                .orElseThrow(() -> notFoundException("id=" + castedRequest.getId()));

        // Partial update — only apply fields that were explicitly provided.
        if (castedRequest.getOperationSystem() != null) {
            plan.setOperationSystem(castedRequest.getOperationSystem());
        }
        if (castedRequest.getUpgradeDrivers() != null) {
            plan.setUpgradeDrivers(castedRequest.getUpgradeDrivers());
        }
        if (castedRequest.getAdditionalPerformanceSoftware() != null) {
            plan.setAdditionalPerformanceSoftware(castedRequest.getAdditionalPerformanceSoftware());
        }
        if (castedRequest.getIssueDescription() != null) {
            plan.setIssueDescription(castedRequest.getIssueDescription());
        }
        if (castedRequest.getScreenShotFilePath() != null) {
            plan.setScreenShotFilePath(castedRequest.getScreenShotFilePath());
        }
        if (castedRequest.getStatus() != null) {
            plan.setStatus(castedRequest.getStatus());
        }

        BasicCarePlan saved = repository.save(plan);
        logger.info("BasicCarePlan updated | id={} status={} totalQuote={}",
                saved.getId(), saved.getStatus(), saved.getTotalQuote());

        evictCache(REDIS_BY_ID + saved.getId());
        evictCache(REDIS_BY_STATUS + saved.getStatus());
        invalidateListCaches();
        return toResponseList(List.of(saved));
    }

    /**
     * Hard-deletes a Basic Care Plan record.
     *
     * <p>This is a full removal — call only when data-retention rules permit.
     * Returns the deleted record snapshot for audit logging on the client side.
     */
    @RateLimiter(name = "basicCareDelete", fallbackMethod = "deleteFallback")
    public List<BasicCarePlanResponse> deleteBasicCarePlan(RequestContract request) {
        if (!(request instanceof DeleteBasicCarePlanRequest castedRequest)) {
            throw badRequestException();
        }
        if (castedRequest.getId() == null || castedRequest.getId() <= 0) {
            ExceptionHandlerReporter.setResolveIssueDetails("Provide a valid positive id.");
            ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
            throw new IncorrectRequestSentException("Invalid id for delete: " + castedRequest.getId());
        }

        logger.info("Deleting BasicCarePlan | id={}", castedRequest.getId());

        BasicCarePlan plan = repository.findById(castedRequest.getId())
                .orElseThrow(() -> notFoundException("id=" + castedRequest.getId()));

        List<BasicCarePlanResponse> snapshot = toResponseList(List.of(plan));
        repository.delete(plan);

        evictCache(REDIS_BY_ID + plan.getId());
        evictCache(REDIS_BY_STATUS + plan.getStatus());
        invalidateListCaches();

        logger.info("BasicCarePlan deleted | id={}", plan.getId());
        return snapshot;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Read operations
    // ══════════════════════════════════════════════════════════════════════

    private List<BasicCarePlanResponse> findAllBasicCarePlans(Pageable pageable) {
        logger.debug("findAll | page={} size={}", pageable.getPageNumber(), pageable.getPageSize());

        boolean isCacheable = pageable.getPageNumber() == 0 && enableRedisUse;

        if (isCacheable) {
            List<BasicCarePlanResponse> cached =
                    redisService.getList(REDIS_FIND_ALL, BasicCarePlanResponse.class);
            if (cached != null && !cached.isEmpty()) {
                logger.debug("Cache hit | key={}", REDIS_FIND_ALL);
                return cached;
            }
        }

        Page<BasicCarePlan> page = repository.findAll(pageable);
        List<BasicCarePlanResponse> result = toResponseList(page.getContent());

        if (isCacheable && !result.isEmpty()) {
            redisService.setList(REDIS_FIND_ALL, result, 12L, TimeUnit.HOURS);
        }

        logger.info("findAll complete | totalElements={} page={} size={}",
                page.getTotalElements(), pageable.getPageNumber(), pageable.getPageSize());
        return result;
    }

    private List<BasicCarePlanResponse> findBasicCarePlanById(RequestContract request) {
        if (!(request instanceof FindBasicCarePlanByIdRequest castedRequest)) {
            throw badRequestException();
        }

        String redisKey = REDIS_BY_ID + castedRequest.getId();
        logger.debug("findById | id={}", castedRequest.getId());

        if (enableRedisUse) {
            List<BasicCarePlanResponse> cached =
                    redisService.getList(redisKey, BasicCarePlanResponse.class);
            if (cached != null && !cached.isEmpty()) {
                logger.debug("Cache hit | key={}", redisKey);
                return cached;
            }
        }

        BasicCarePlan found = repository.findById(castedRequest.getId())
                .orElseThrow(() -> notFoundException("id=" + castedRequest.getId()));

        List<BasicCarePlanResponse> result = toResponseList(List.of(found));
        cacheList(redisKey, result, 12L, TimeUnit.HOURS);
        return result;
    }

    /**
     * Returns plans filtered by lifecycle status, with caching on the first page.
     */
    private List<BasicCarePlanResponse> findBasicCarePlansByStatus(RequestContract request) {
        if (!(request instanceof FindBasicCarePlansByStatusRequest castedRequest)) {
            throw badRequestException();
        }

        // Validate code is a known status.
        BasicCareStatus.fromCode(castedRequest.getStatus());

        Pageable  pageable    = castedRequest.toPageable();
        String    redisKey    = REDIS_BY_STATUS + castedRequest.getStatus();
        boolean   isCacheable = pageable.getPageNumber() == 0 && enableRedisUse;

        logger.debug("findByStatus | status={} page={} size={}",
                castedRequest.getStatus(), pageable.getPageNumber(), pageable.getPageSize());

        if (isCacheable) {
            List<BasicCarePlanResponse> cached =
                    redisService.getList(redisKey, BasicCarePlanResponse.class);
            if (cached != null && !cached.isEmpty()) {
                logger.debug("Cache hit | key={}", redisKey);
                return cached;
            }
        }

        Page<BasicCarePlan>    page   = repository.findByStatus(castedRequest.getStatus(), pageable);
        List<BasicCarePlanResponse> result = toResponseList(page.getContent());

        if (isCacheable && !result.isEmpty()) {
            redisService.setList(redisKey, result, 1L, TimeUnit.HOURS);
        }

        logger.info("findByStatus complete | status={} count={}",
                castedRequest.getStatus(), result.size());
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Rate-limiter fallbacks
    // ══════════════════════════════════════════════════════════════════════

    public List<BasicCarePlanResponse> createFallback(
            RequestContract request, RequestNotPermitted ex) {
        logger.warn("Rate limit triggered | limiter=basicCareCreate message={}", ex.getMessage());
        ExceptionHandlerReporter.setResolveIssueDetails(
                "Too many plan creation requests. Please wait before trying again.");
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        throw new IncorrectRequestSentException("Too many create requests. Please slow down.");
    }

    public List<BasicCarePlanResponse> updateFallback(
            RequestContract request, RequestNotPermitted ex) {
        logger.warn("Rate limit triggered | limiter=basicCareUpdate message={}", ex.getMessage());
        ExceptionHandlerReporter.setResolveIssueDetails(
                "Too many plan update requests. Please wait before trying again.");
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        throw new IncorrectRequestSentException("Too many update requests. Please slow down.");
    }

    public List<BasicCarePlanResponse> deleteFallback(
            RequestContract request, RequestNotPermitted ex) {
        logger.warn("Rate limit triggered | limiter=basicCareDelete message={}", ex.getMessage());
        ExceptionHandlerReporter.setResolveIssueDetails(
                "Too many plan delete requests. Please wait before trying again.");
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        throw new IncorrectRequestSentException("Too many delete requests. Please slow down.");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════

    private List<BasicCarePlanResponse> toResponseList(List<BasicCarePlan> plans) {
        return plans.stream()
                .map(p -> {
                    BasicCareStatus statusEnum = BasicCareStatus.fromCode(p.getStatus());
                    return BasicCarePlanResponse.builder()
                            .id(p.getId())
                            // OS
                            .operationSystem(p.getOperationSystem())
                            .operationSystemDisplayName(
                                    p.getOperationSystem() != null
                                            ? p.getOperationSystem().getDisplayName() : null)
                            .operationSystemPrice(
                                    p.getOperationSystem() != null
                                            ? p.getOperationSystem().getPrice() : null)
                            // Drivers
                            .upgradeDrivers(p.getUpgradeDrivers())
                            .upgradeDriversDisplayName(
                                    p.getUpgradeDrivers() != null
                                            ? p.getUpgradeDrivers().getDisplayName() : null)
                            .upgradeDriversPrice(
                                    p.getUpgradeDrivers() != null
                                            ? p.getUpgradeDrivers().getPrice() : null)
                            // Software
                            .additionalPerformanceSoftware(p.getAdditionalPerformanceSoftware())
                            .additionalPerformanceSoftwareDisplayName(
                                    p.getAdditionalPerformanceSoftware() != null
                                            ? p.getAdditionalPerformanceSoftware().getDisplayName() : null)
                            .additionalPerformanceSoftwarePrice(
                                    p.getAdditionalPerformanceSoftware() != null
                                            ? p.getAdditionalPerformanceSoftware().getPrice() : null)
                            // Details
                            .issueDescription(p.getIssueDescription())
                            .screenShotFilePath(p.getScreenShotFilePath())
                            // Quote
                            .totalQuote(p.getTotalQuote())
                            // Status
                            .status(p.getStatus())
                            .statusEnum(statusEnum)
                            .statusDisplayName(statusEnum.getDisplayName())
                            // Timestamps
                            .createdDate(p.getCreatedDate())
                            .modifiedDate(p.getModifiedDate())
                            .build();
                })
                .toList();
    }

    /**
     * Evicts all list-level caches that become stale after any write mutation.
     */
    private void invalidateListCaches() {
        evictCache(REDIS_FIND_ALL);
        // Status-scoped caches are evicted individually per write; no bulk-clear needed.
    }

    private void cacheList(String key, List<BasicCarePlanResponse> list,
                           long duration, TimeUnit unit) {
        if (enableRedisUse && !list.isEmpty()) {
            redisService.setList(key, list, duration, unit);
            logger.debug("Cache populated | key={} entries={}", key, list.size());
        }
    }

    private void evictCache(String key) {
        if (enableRedisUse) {
            redisService.delete(key);
            logger.debug("Cache evicted | key={}", key);
        }
    }

    private Pageable toPageable(RequestContract request) {
        if (request instanceof PageableRequest pageableRequest) {
            return pageableRequest.toPageable();
        }
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
        return new IncorrectRequestSentException(
                "BasicCarePlan not found for: " + criteria);
    }
}
