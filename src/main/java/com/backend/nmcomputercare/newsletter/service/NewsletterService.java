package com.backend.nmcomputercare.newsletter.service;

import com.backend.nmcomputercare.newsletter.dtos.*;
import com.backend.nmcomputercare.newsletter.entity.Newsletter;
import com.backend.nmcomputercare.newsletter.repository.NewsletterRepository;

import com.backend.nmcomputercare.newsletter.validaator.NewsletterValidator;
import com.backend.nmcomputercare.utils.*;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import com.backend.nmcomputercare.utils.exceptions.ServiceNameNotFoundException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service handling all Newsletter operations including creation, retrieval,
 * Redis caching, and Resilience4j rate limiting.
 *
 * <p>Follows the same dispatcher pattern as {@code ContactFormService}:
 * the public {@link #callable} method routes by {@code serviceName}, while
 * write operations route through {@code self} to honour the
 * {@code @RateLimiter} AOP proxy.
 */
@RequiredArgsConstructor
@Data
@Service
@Transactional
public class NewsletterService implements ExecuteService {

    private static final Logger logger = LoggerFactory.getLogger(NewsletterService.class);

    // ── Redis key prefixes ──────────────────────────────────────────────────────
    private static final String REDIS_NS           = "NEWSLETTER:";
    private static final String REDIS_FIND_ALL     = REDIS_NS + "FIND_ALL";
    private static final String REDIS_BY_ID        = REDIS_NS + "BY_ID:";
    private static final String REDIS_BY_CATEGORY  = REDIS_NS + "BY_CATEGORY:";

    // ── Shared exception messages ───────────────────────────────────────────────
    private static final String BAD_REQUEST_MSG = "Request contract type mismatch.";
    private static final String BAD_REQUEST_RES = "Ensure the correct RequestContract subtype is passed for this service.";
    private static final String NOT_FOUND_RES   = "No matching records exist for the given criteria.";

    @Value("${redis.setting.enable:false}")
    private boolean enableRedisUse;

    private final NewsletterRepository repository;
    private final RedisService         redisService;
    private final ExceptionAdvice      advice;
    private final NewsletterValidator validator;


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
                // Route create through self-proxy so @RateLimiter AOP fires.
                case "createNewsletter" -> ((NewsletterService) AopContext.currentProxy()).createNewsletter(request);
                case "findAllNewsletters"        -> findAllNewsletters(toPageable(request));
                case "findNewsletterById"        -> findNewsletterById(request);
                default -> {
                    String msg = "Service name not found: " + serviceName;
                    logger.warn("Unknown service requested | service={} correlationId={}", serviceName, correlationId);
                    throw advice.throwExceptionAndAdvice(
                            new ServiceNameNotFoundException(msg), msg,
                            "Ensure the service name matches one of the defined cases.");
                }
            };
        } finally {
            logger.debug("Service dispatch complete | service={} correlationId={}", serviceName, correlationId);
            MDC.clear();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Write operation
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Creates and persists a new newsletter draft.
     *
     * <p>Rate-limited via Resilience4j — configure the limiter named
     * {@code "newsletterCreate"} in {@code application.yml}.
     *
     * <p><b>Must be {@code public}</b> for Spring AOP proxying.
     * Always call via {@code self.createNewsletter()} inside this class.
     */
    @RateLimiter(name = "newsletterCreate", fallbackMethod = "createNewsletterFallback")
    public List<NewsletterResponse> createNewsletter(RequestContract request) {
        if (!(request instanceof NewsletterRequest castedRequest)) {
            throw badRequestException();
        }

        validator.validate(castedRequest);


        Newsletter entity = Newsletter.builder()
                .title(castedRequest.getTitle())
                .content(castedRequest.getContent())
                .createdDate(LocalDateTime.now())
                .status((byte) 0)   // 0 = draft
                .build();

        Newsletter saved = repository.save(entity);
        logger.info("Newsletter saved | id={} title={}", saved.getId(), saved.getTitle());

        // Invalidate the FIND_ALL cache so stale data is not served.
        evictCache(REDIS_FIND_ALL);

        return toResponseList(List.of(saved));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Read operations
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Returns a paginated list of all newsletters.
     * Only the first page is cached to keep Redis payloads bounded.
     */
    private List<NewsletterResponse> findAllNewsletters(Pageable pageable) {
        logger.debug("findAll | page={} size={}", pageable.getPageNumber(), pageable.getPageSize());

        boolean isCacheable = pageable.getPageNumber() == 0 && enableRedisUse;

        if (isCacheable) {
            List<NewsletterResponse> cached = redisService.getList(REDIS_FIND_ALL, NewsletterResponse.class);
            if (cached != null && !cached.isEmpty()) {
                logger.debug("Cache hit | key={}", REDIS_FIND_ALL);
                return cached;
            }
        }

        Page<Newsletter> page = repository.findAll(pageable);
        List<NewsletterResponse> result = toResponseList(page.getContent());

        if (isCacheable && !result.isEmpty()) {
            redisService.setList(REDIS_FIND_ALL, result, 24L, TimeUnit.HOURS);
        }

        logger.info("findAll complete | totalElements={} page={} size={}",
                page.getTotalElements(), pageable.getPageNumber(), pageable.getPageSize());

        return result;
    }

    private List<NewsletterResponse> findNewsletterById(RequestContract request) {
        if (!(request instanceof FindNewsletterByIdRequest castedRequest)) {
            throw badRequestException();
        }

        String redisKey = REDIS_BY_ID + castedRequest.getId();
        logger.debug("findById | id={}", castedRequest.getId());

        if (enableRedisUse) {
            List<NewsletterResponse> cached = redisService.getList(redisKey, NewsletterResponse.class);
            if (cached != null && !cached.isEmpty()) {
                logger.debug("Cache hit | key={}", redisKey);
                return cached;
            }
        }

        Optional<Newsletter> found = repository.findById(castedRequest.getId());
        if (found.isEmpty()) {
            logger.warn("No newsletter found | id={}", castedRequest.getId());
            String msg = "No newsletter found for id: " + castedRequest.getId();
            throw advice.throwExceptionAndAdvice(new IncorrectRequestSentException(msg), msg, NOT_FOUND_RES);
        }

        List<NewsletterResponse> result = toResponseList(found.stream().toList());
        cacheList(redisKey, result);
        return result;
    }



    // ══════════════════════════════════════════════════════════════════════════
    //  Rate-limiter fallback
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Invoked automatically by Resilience4j when {@code createNewsletter}
     * exceeds its configured rate limit.
     */
    public List<NewsletterResponse> createNewsletterFallback(
            RequestContract request, RequestNotPermitted ex) {

        logger.warn("Rate limit triggered | limiter=newsletterCreate message={}", ex.getMessage());

        throw advice.throwExceptionAndAdvice(
                new IncorrectRequestSentException("Too many newsletter creation requests."),
                "Too many newsletter creation requests.",
                "You have exceeded the allowed creation rate. Please wait before trying again.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private List<NewsletterResponse> toResponseList(List<Newsletter> newsletters) {
        return newsletters.stream()
                .map(n -> NewsletterResponse.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .content(n.getContent())
                        .createdDate(n.getCreatedDate())
                        .status(n.getStatus())
                        .build())
                .toList();
    }

    private void cacheList(String key, List<NewsletterResponse> list) {
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

    private RuntimeException badRequestException() {
        return advice.throwExceptionAndAdvice(
                new IncorrectRequestSentException(BAD_REQUEST_MSG),
                BAD_REQUEST_MSG, BAD_REQUEST_RES);
    }
}