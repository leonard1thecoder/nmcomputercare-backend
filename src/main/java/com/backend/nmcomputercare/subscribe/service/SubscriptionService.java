package com.backend.nmcomputercare.subscribe.service;

import com.backend.nmcomputercare.subscribe.dtos.FindSubscriptionByEmailRequest;
import com.backend.nmcomputercare.subscribe.dtos.SubscribeRequest;
import com.backend.nmcomputercare.subscribe.dtos.SubscriptionResponse;
import com.backend.nmcomputercare.subscribe.dtos.UnsubscribeRequest;
import com.backend.nmcomputercare.subscribe.entity.Subscription;
import com.backend.nmcomputercare.subscribe.repository.SubscriptionRepository;
import com.backend.nmcomputercare.subscribe.validaator.SubscriptionValidator;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service handling all Subscription operations: subscribe, unsubscribe,
 * paginated retrieval, Redis caching, and Resilience4j rate limiting.
 *
 * <p>Subscribe / unsubscribe are rate-limited independently — configure
 * {@code "subscriptionCreate"} and {@code "subscriptionCancel"}
 * in {@code application.yml}.
 */
@RequiredArgsConstructor
@Data
@Service
@Transactional
public class SubscriptionService implements ExecuteService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    // ── Redis key prefixes ──────────────────────────────────────────────────────
    private static final String REDIS_NS       = "SUBSCRIPTION:";
    private static final String REDIS_FIND_ALL = REDIS_NS + "FIND_ALL";
    private static final String REDIS_BY_EMAIL = REDIS_NS + "BY_EMAIL:";
    private static final String REDIS_ACTIVE   = REDIS_NS + "ACTIVE";

    // ── Shared messages ─────────────────────────────────────────────────────────
    private static final String BAD_REQUEST_MSG     = "Request contract type mismatch.";
    private static final String BAD_REQUEST_DETAILS = "Ensure the correct RequestContract subtype is passed for this service.";
    private static final String NOT_FOUND_DETAILS   = "No matching records exist for the given criteria.";

    @Value("${redis.setting.enable:false}")
    private boolean enableRedisUse;

    private final SubscriptionRepository repository;
    private final RedisService           redisService;
    private final SubscriptionValidator  validator;


    // ══════════════════════════════════════════════════════════════════════════
    //  Dispatcher
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public List<? extends ResponseContract> callable(String serviceName, RequestContract request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("serviceName", serviceName);

        try {
            logger.info("Service invoked | service={} correlationId={}", serviceName, correlationId);

            return switch (serviceName) {
                // Route writes through the self-proxy so @RateLimiter fires.
                case "subscribe"   -> ((SubscriptionService) AopContext.currentProxy()).subscribe(request);
                case "unsubscribe" -> ((SubscriptionService) AopContext.currentProxy()).unsubscribe(request);

                case "findAllSubscriptions"    -> findAllSubscriptions(toPageable(request));
                case "findSubscriptionByEmail" -> findSubscriptionByEmail(request);
                case "findActiveSubscriptions" -> findActiveSubscriptions(toPageable(request));
                default -> {
                    String msg = "Service name not found: " + serviceName;
                    logger.warn("Unknown service requested | service={} correlationId={}", serviceName, correlationId);
                    ExceptionHandlerReporter.setResolveIssueDetails("Ensure the service name matches one of the defined cases.");
                    ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
                    throw new ServiceNameNotFoundException(msg);
                }
            };
        } finally {
            logger.debug("Service dispatch complete | service={} correlationId={}", serviceName, correlationId);
            MDC.clear();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Write operations
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Opts an email address into the newsletter.
     *
     * <ul>
     *   <li>Re-activates a previously unsubscribed address rather than
     *       creating a duplicate row.</li>
     *   <li>Throws a meaningful exception when the address is already active.</li>
     * </ul>
     */
    @RateLimiter(name = "subscriptionCreate", fallbackMethod = "subscribeFallback")
    public List<SubscriptionResponse> subscribe(RequestContract request) {
        if (!(request instanceof SubscribeRequest castedRequest)) {
            throw badRequestException();
        }

        validator.validate(castedRequest);

        logger.info("Subscribe request | email={}", maskEmail(castedRequest.getEmail()));

        // Re-activate a lapsed subscription rather than creating a duplicate.
        Optional<Subscription> existing = repository.findByEmail(castedRequest.getEmail());

        if (existing.isPresent()) {
            Subscription sub = existing.get();
            if (sub.isActive()) {
                String msg = "Email is already subscribed: " + castedRequest.getEmail();
                ExceptionHandlerReporter.setResolveIssueDetails("This email address is already an active subscriber.");
                ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
                throw new IncorrectRequestSentException(msg);
            }
            // Re-activate.
            sub.setActive(true);
            sub.setUnsubscribedDate(null);
            sub.setSubscribedDate(LocalDateTime.now());
            sub.setStatus((byte) 0);    // pending re-confirmation
            Subscription saved = repository.save(sub);
            invalidateSubscriptionCaches(saved.getEmail());
            logger.info("Subscription re-activated | id={} email={}", saved.getId(), maskEmail(saved.getEmail()));
            return toResponseList(List.of(saved));
        }

        Subscription entity = Subscription.builder()
                .email(castedRequest.getEmail())
                .subscribedDate(LocalDateTime.now())
                .active(true)
                .status((byte) 0)   // 0 = pending confirmation
                .build();

        Subscription saved = repository.save(entity);
        logger.info("Subscription created | id={} email={}", saved.getId(), maskEmail(saved.getEmail()));

        invalidateSubscriptionCaches(saved.getEmail());
        return toResponseList(List.of(saved));
    }

    /**
     * Soft-deletes a subscription: sets {@code active = false} and records the
     * unsubscribe timestamp.  The row is never physically removed.
     */
    @RateLimiter(name = "subscriptionCancel", fallbackMethod = "unsubscribeFallback")
    public List<SubscriptionResponse> unsubscribe(RequestContract request) {
        if (!(request instanceof UnsubscribeRequest castedRequest)) {
            throw badRequestException();
        }

        logger.info("Unsubscribe request | email={}", maskEmail(castedRequest.getEmail()));

        Subscription sub = repository.findByEmail(castedRequest.getEmail())
                .orElseThrow(() -> {
                    String msg = "No subscription found for email: " + castedRequest.getEmail();
                    ExceptionHandlerReporter.setResolveIssueDetails(NOT_FOUND_DETAILS);
                    ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
                    return new IncorrectRequestSentException(msg);
                });

        if (!sub.isActive()) {
            ExceptionHandlerReporter.setResolveIssueDetails("This email address is not currently subscribed.");
            ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
            throw new IncorrectRequestSentException(
                    "Subscription is already inactive for email: " + castedRequest.getEmail());
        }

        sub.setActive(false);
        sub.setUnsubscribedDate(LocalDateTime.now());
        sub.setStatus((byte) 2);    // 2 = unsubscribed

        Subscription saved = repository.save(sub);
        logger.info("Subscription cancelled | id={} email={}", saved.getId(), maskEmail(saved.getEmail()));

        invalidateSubscriptionCaches(saved.getEmail());
        return toResponseList(List.of(saved));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Read operations
    // ══════════════════════════════════════════════════════════════════════════

    private List<SubscriptionResponse> findAllSubscriptions(Pageable pageable) {
        logger.debug("findAll | page={} size={}", pageable.getPageNumber(), pageable.getPageSize());

        boolean isCacheable = pageable.getPageNumber() == 0 && enableRedisUse;

        if (isCacheable) {
            List<SubscriptionResponse> cached = redisService.getList(REDIS_FIND_ALL, SubscriptionResponse.class);
            if (cached != null && !cached.isEmpty()) {
                logger.debug("Cache hit | key={}", REDIS_FIND_ALL);
                return cached;
            }
        }

        Page<Subscription> page = repository.findAll(pageable);
        List<SubscriptionResponse> result = toResponseList(page.getContent());

        if (isCacheable && !result.isEmpty()) {
            redisService.setList(REDIS_FIND_ALL, result, 24L, TimeUnit.HOURS);
        }

        logger.info("findAll complete | totalElements={} page={} size={}",
                page.getTotalElements(), pageable.getPageNumber(), pageable.getPageSize());

        return result;
    }

    private List<SubscriptionResponse> findSubscriptionByEmail(RequestContract request) {
        if (!(request instanceof FindSubscriptionByEmailRequest castedRequest)) {
            throw badRequestException();
        }

        String redisKey = REDIS_BY_EMAIL + castedRequest.getEmail();
        logger.debug("findByEmail | email={}", maskEmail(castedRequest.getEmail()));

        if (enableRedisUse) {
            List<SubscriptionResponse> cached = redisService.getList(redisKey, SubscriptionResponse.class);
            if (cached != null && !cached.isEmpty()) {
                logger.debug("Cache hit | key={}", redisKey);
                return cached;
            }
        }

        Optional<Subscription> found = repository.findByEmail(castedRequest.getEmail());
        if (found.isEmpty()) {
            logger.warn("No subscription found | email={}", maskEmail(castedRequest.getEmail()));
            ExceptionHandlerReporter.setResolveIssueDetails(NOT_FOUND_DETAILS);
            ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
            throw new IncorrectRequestSentException(
                    "No subscription found for email: " + castedRequest.getEmail());
        }

        List<SubscriptionResponse> result = toResponseList(found.stream().toList());
        cacheList(redisKey, result);
        return result;
    }

    /**
     * Returns only active subscribers.  Cached separately from findAll so that
     * subscribe / unsubscribe actions can invalidate both caches independently.
     */
    private List<SubscriptionResponse> findActiveSubscriptions(Pageable pageable) {
        logger.debug("findActive | page={} size={}", pageable.getPageNumber(), pageable.getPageSize());

        boolean isCacheable = pageable.getPageNumber() == 0 && enableRedisUse;

        if (isCacheable) {
            List<SubscriptionResponse> cached = redisService.getList(REDIS_ACTIVE, SubscriptionResponse.class);
            if (cached != null && !cached.isEmpty()) {
                logger.debug("Cache hit | key={}", REDIS_ACTIVE);
                return cached;
            }
        }

        List<Subscription> found = repository.findByActive(true);
        List<SubscriptionResponse> result = toResponseList(found);

        if (isCacheable && !result.isEmpty()) {
            redisService.setList(REDIS_ACTIVE, result, 1L, TimeUnit.HOURS);
        }

        logger.info("findActive complete | count={}", result.size());
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Rate-limiter fallbacks
    // ══════════════════════════════════════════════════════════════════════════

    public List<SubscriptionResponse> subscribeFallback(
            RequestContract request, RequestNotPermitted ex) {

        logger.warn("Rate limit triggered | limiter=subscriptionCreate message={}", ex.getMessage());
        ExceptionHandlerReporter.setResolveIssueDetails("You have exceeded the allowed subscription rate. Please wait before trying again.");
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        throw new IncorrectRequestSentException("Too many subscription requests.");
    }

    public List<SubscriptionResponse> unsubscribeFallback(
            RequestContract request, RequestNotPermitted ex) {

        logger.warn("Rate limit triggered | limiter=subscriptionCancel message={}", ex.getMessage());
        ExceptionHandlerReporter.setResolveIssueDetails("You have exceeded the allowed cancellation rate. Please wait before trying again.");
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        throw new IncorrectRequestSentException("Too many unsubscribe requests.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private List<SubscriptionResponse> toResponseList(List<Subscription> subs) {
        return subs.stream()
                .map(s -> SubscriptionResponse.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .email(s.getEmail())
                        .subscribedDate(s.getSubscribedDate())
                        .unsubscribedDate(s.getUnsubscribedDate())
                        .active(s.isActive())
                        .status(s.getStatus())
                        .build())
                .toList();
    }

    /**
     * Evicts all caches that could now be stale after a subscribe / unsubscribe
     * mutation for the given email address.
     */
    private void invalidateSubscriptionCaches(String email) {
        evictCache(REDIS_FIND_ALL);
        evictCache(REDIS_ACTIVE);
        evictCache(REDIS_BY_EMAIL + email);
    }

    private void cacheList(String key, List<SubscriptionResponse> list) {
        if (enableRedisUse && !list.isEmpty()) {
            redisService.setList(key, list, 24L, TimeUnit.HOURS);
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

    /**
     * Centralises the bad-request reporter setup and throw so every
     * instanceof guard stays a clean one-liner.
     */
    private RuntimeException badRequestException() {
        ExceptionHandlerReporter.setResolveIssueDetails(BAD_REQUEST_DETAILS);
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        return new IncorrectRequestSentException(BAD_REQUEST_MSG);
    }

    // ── PII masking ────────────────────────────────────────────────────────────

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local  = parts[0];
        String masked = local.length() > 2 ? local.substring(0, 2) + "***" : "***";
        return masked + "@" + parts[1];
    }
}