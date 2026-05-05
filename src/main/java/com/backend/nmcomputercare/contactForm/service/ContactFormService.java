package com.backend.nmcomputercare.contactForm.service;

import com.backend.nmcomputercare.contactForm.dtos.*;
import com.backend.nmcomputercare.contactForm.entity.ContactForm;
import com.backend.nmcomputercare.contactForm.mailing.dto.ContactFormEmailEvent;
import com.backend.nmcomputercare.contactForm.repository.ContactFormRepository;
import com.backend.nmcomputercare.contactForm.validator.ContactFormValidator;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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
 * Service handling all ContactForm operations including submission, retrieval,
 * caching via Redis, rate limiting, and paginated responses.
 */
@RequiredArgsConstructor
@Data
@Service
@Transactional
public class ContactFormService implements ExecuteService {

    private static final Logger logger = LoggerFactory.getLogger(ContactFormService.class);

    // ── Redis key prefixes ──────────────────────────────────────────────────────
    private static final String REDIS_NS        = "CUSTOMER:CONTACT_FORM:";
    private static final String REDIS_FIND_ALL  = REDIS_NS + "FIND_ALL";
    private static final String REDIS_BY_ID     = REDIS_NS + "BY_ID:";
    private static final String REDIS_BY_EMAIL  = REDIS_NS + "BY_EMAIL:";
    private static final String REDIS_BY_NUMBER = REDIS_NS + "BY_NUMBER:";

    // ── Shared exception messages ───────────────────────────────────────────────
    private static final String BAD_REQUEST_MSG = "Request contract type mismatch.";
    private static final String BAD_REQUEST_RES = "Ensure the correct RequestContract subtype is passed for this service.";
    private static final String NOT_FOUND_RES   = "No matching records exist for the given criteria.";

    @Value("${redis.setting.enable:false}")
    private boolean enableRedisUse;

    private final ApplicationEventPublisher publisher;
    private final RedisService              redisService;
    private final ContactFormRepository     repository;
    private final ExceptionAdvice           advice;
    private final ContactFormValidator      validator;

    /**
     * Self-reference injected lazily so that calls to {@link #sendCustomerRequest}
     * pass through the Spring AOP proxy, allowing {@code @RateLimiter} to intercept
     * them.  Without this, internal {@code this.} calls bypass the proxy entirely
     * and the annotation has no effect.
     */
    @Lazy
    @Setter(onMethod_ = @Autowired)
    private ContactFormService self;

    // ══════════════════════════════════════════════════════════════════════════
    //  Public dispatch entry-point (ExecuteService contract)
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public List<? extends ResponseContract> callable(String serviceName, RequestContract request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("serviceName", serviceName);

        try {
            logger.info("Service invoked | service={} correlationId={}", serviceName, correlationId);

            return switch (serviceName) {
                // Route through self-proxy so @RateLimiter AOP interception fires.
                case "sendCustomerRequest"          -> self.sendCustomerRequest(request);
                case "findAllCustomerRequest"       -> findAllCustomerRequest(toPageable(request));
                case "findCustomerRequestById"      -> findCustomerRequestById(request);
                case "findCustomerRequestByEmail"   -> findCustomerRequestByEmail(request);
                case "findCustomerRequestByNumbers" -> findCustomerRequestByNumbers(request);
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
     * Accepts a contact form submission.
     *
     * <p>Rate-limited via Resilience4j — configure the limiter named
     * {@code "contactFormSubmit"} in {@code application.yml}.
     * When the limit is breached Resilience4j throws {@link RequestNotPermitted},
     * which is caught by the fallback below and re-thrown as a meaningful
     * application exception.
     *
     * <p><b>Must be {@code public}</b> so Spring AOP can proxy the call.
     * Invoke exclusively via {@code self.sendCustomerRequest()} inside this class.
     */
    @RateLimiter(name = "contactFormSubmit", fallbackMethod = "sendCustomerRequestFallback")
    public List<ContactFormResponse> sendCustomerRequest(RequestContract request) {
        if (!(request instanceof ContactFormRequest castedRequest)) {
            throw badRequestException();
        }

        validator.validate(castedRequest);

        logger.info("Saving contact form | email={} service={}",
                maskEmail(castedRequest.getEmail()), castedRequest.getService());

        ContactForm entity = ContactForm.builder()
                .name(castedRequest.getName())
                .email(castedRequest.getEmail())
                .numbers(castedRequest.getNumbers())
                .service(castedRequest.getService())
                .message(castedRequest.getMessage())
                .sentDate(LocalDateTime.now())
                .status((byte) 0)
                .build();

        publishEmailEvents(castedRequest);

        ContactForm saved = repository.save(entity);
        logger.info("Contact form saved | id={} email={}", saved.getId(), maskEmail(saved.getEmail()));

        // Invalidate the FIND_ALL cache so stale data is not served.
        evictCache(REDIS_FIND_ALL);

        return toResponseList(List.of(saved));
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Read operations
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Returns a paginated list of all contact form records.
     * Pagination is bypassed for Redis cache hits to keep cache payloads bounded;
     * only the first page is cached.
     */
    private List<ContactFormResponse> findAllCustomerRequest(Pageable pageable) {
        logger.debug("findAll | page={} size={}", pageable.getPageNumber(), pageable.getPageSize());

        // Cache only the default first page to avoid unbounded cache entries.
        boolean isCacheable = pageable.getPageNumber() == 0 && enableRedisUse;

        if (isCacheable) {
            List<ContactFormResponse> cached = redisService.getList(REDIS_FIND_ALL, ContactFormResponse.class);
            if (cached != null && !cached.isEmpty()) {
                logger.debug("Cache hit | key={}", REDIS_FIND_ALL);
                return cached;
            }
        }

        Page<ContactForm> page = repository.findAll(pageable);
        List<ContactFormResponse> result = toResponseList(page.getContent());

        if (isCacheable && !result.isEmpty()) {
            redisService.setList(REDIS_FIND_ALL, result, 24L, TimeUnit.HOURS);
        }

        logger.info("findAll complete | totalElements={} page={} size={}",
                page.getTotalElements(), pageable.getPageNumber(), pageable.getPageSize());

        return result;
    }

    private List<ContactFormResponse> findCustomerRequestById(RequestContract request) {
        if (!(request instanceof FindContactFormByIdRequest castedRequest)) {
            throw badRequestException();
        }

        String redisKey = REDIS_BY_ID + castedRequest.getId();
        logger.debug("findById | id={}", castedRequest.getId());

        if (enableRedisUse) {
            List<ContactFormResponse> cached = redisService.getList(redisKey, ContactFormResponse.class);
            if (cached != null && !cached.isEmpty()) {
                logger.debug("Cache hit | key={}", redisKey);
                return cached;
            }
        }

        Optional<ContactForm> found = repository.findById(castedRequest.getId());
        if (found.isEmpty()) {
            logger.warn("No record found | id={}", castedRequest.getId());
            String msg = "No contact form found for id: " + castedRequest.getId();
            throw advice.throwExceptionAndAdvice(new IncorrectRequestSentException(msg), msg, NOT_FOUND_RES);
        }

        List<ContactFormResponse> result = toResponseList(found.stream().toList());
        cacheList(redisKey, result);
        return result;
    }

    private List<ContactFormResponse> findCustomerRequestByEmail(RequestContract request) {
        if (!(request instanceof FindContactFormByEmailRequest castedRequest)) {
            throw badRequestException();
        }

        String redisKey = REDIS_BY_EMAIL + castedRequest.getEmail();
        logger.debug("findByEmail | email={}", maskEmail(castedRequest.getEmail()));

        if (enableRedisUse) {
            List<ContactFormResponse> cached = redisService.getList(redisKey, ContactFormResponse.class);
            if (cached != null && !cached.isEmpty()) {
                logger.debug("Cache hit | key={}", redisKey);
                return cached;
            }
        }

        List<ContactForm> found = repository.findByEmail(castedRequest.getEmail());
        if (found.isEmpty()) {
            logger.warn("No records found | email={}", maskEmail(castedRequest.getEmail()));
            String msg = "No contact forms found for email: " + castedRequest.getEmail();
            throw advice.throwExceptionAndAdvice(new IncorrectRequestSentException(msg), msg, NOT_FOUND_RES);
        }

        List<ContactFormResponse> result = toResponseList(found);
        cacheList(redisKey, result);
        return result;
    }

    private List<ContactFormResponse> findCustomerRequestByNumbers(RequestContract request) {
        if (!(request instanceof FindContactFormByNumbersRequest castedRequest)) {
            throw badRequestException();
        }

        String redisKey = REDIS_BY_NUMBER + castedRequest.getNumbers();
        logger.debug("findByNumbers | numbers={}", maskPhone(castedRequest.getNumbers()));

        if (enableRedisUse) {
            List<ContactFormResponse> cached = redisService.getList(redisKey, ContactFormResponse.class);
            if (cached != null && !cached.isEmpty()) {
                logger.debug(" xCache hit | key={}", redisKey);
                return cached;
            }
        }

        List<ContactForm> found = repository.findByNumbers(castedRequest.getNumbers());
        if (found.isEmpty()) {
            logger.warn("No records found | numbers={}", maskPhone(castedRequest.getNumbers()));
            String msg = "No contact forms found for numbers: " + castedRequest.getNumbers();
            throw advice.throwExceptionAndAdvice(new IncorrectRequestSentException(msg), msg, NOT_FOUND_RES);
        }

        List<ContactFormResponse> result = toResponseList(found);
        cacheList(redisKey, result);
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Rate-limiter fallback
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Invoked automatically by Resilience4j when {@code sendCustomerRequest}
     * exceeds its rate limit.
     *
     * <p>Fallback signature rules:
     * <ul>
     *   <li>Same name as the guarded method + "Fallback" suffix.</li>
     *   <li>Same parameter list, plus an extra trailing {@link RequestNotPermitted}.</li>
     *   <li>Same return type.</li>
     * </ul>
     */
    public List<ContactFormResponse> sendCustomerRequestFallback(
            RequestContract request, RequestNotPermitted ex) {

        logger.warn("Rate limit triggered | limiter=contactFormSubmit message={}",
                ex.getMessage());

        throw advice.throwExceptionAndAdvice(
                new IncorrectRequestSentException("Too many requests submitted."),
                "Too many requests submitted.",
                "You have exceeded the allowed submission rate. Please wait before trying again.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════════

    /** Publish confirmation email to both internal recipients. */
    private void publishEmailEvents(ContactFormRequest req) {
        List.of("kellyNtuli03@gmail.com", "leonard1thecoder@gmail.com")
                .forEach(recipient -> {
                    logger.debug("Publishing email event | recipient={}", maskEmail(recipient));
                    publisher.publishEvent(ContactFormEmailEvent.builder()
                            .email(req.getEmail())
                            .emailSentTo(recipient)
                            .name(req.getName())
                            .phone(req.getNumbers())
                            .service(req.getService())
                            .message(req.getMessage())
                            .build());
                });

        publisher.publishEvent(ContactFormEmailEvent.builder()
                .email(req.getEmail())
                .emailSentTo(req.getEmail())
                .name(req.getName())
                .phone(req.getNumbers())
                .service(req.getService())
                .message(req.getMessage())
                .build());
    }

    /** Map a list of JPA entities → response DTOs. */
    private List<ContactFormResponse> toResponseList(List<ContactForm> forms) {
        return forms.stream()
                .map(f -> ContactFormResponse.builder()
                        .id(f.getId())
                        .name(f.getName())
                        .email(f.getEmail())
                        .numbers(f.getNumbers())
                        .message(f.getMessage())
                        .sentDate(f.getSentDate())
                        .service(f.getService())
                        .status(f.getStatus())
                        .build())
                .toList();
    }

    /** Store a list in Redis only when caching is enabled and the list is non-empty. */
    private void cacheList(String key, List<ContactFormResponse> list) {
        if (enableRedisUse && !list.isEmpty()) {
            redisService.setList(key, list, 24L, TimeUnit.HOURS);
            logger.debug("Cache populated | key={} entries={}", key, list.size());
        }
    }

    /** Evict a single Redis key, ignoring errors if the key doesn't exist. */
    private void evictCache(String key) {
        if (enableRedisUse) {
            redisService.delete(key);
            logger.debug("Cache evicted | key={}", key);
        }
    }

    /**
     * Extract a {@link Pageable} from the request contract when supported,
     * falling back to a first-page default so legacy callers are unaffected.
     */
    private Pageable toPageable(RequestContract request) {
        if (request instanceof PageableRequest pageableRequest) {
            return pageableRequest.toPageable();
        }
        return org.springframework.data.domain.PageRequest.of(0, 20);
    }

    /** Consistent bad-request exception for contract type mismatches. */
    private RuntimeException badRequestException() {
        return advice.throwExceptionAndAdvice(
                new IncorrectRequestSentException(BAD_REQUEST_MSG),
                BAD_REQUEST_MSG, BAD_REQUEST_RES);
    }

    // ── PII masking (logs must never contain raw PII in production) ────────────

    /** e.g. john.doe@example.com → jo***@example.com */
    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        String masked = local.length() > 2
                ? local.substring(0, 2) + "***"
                : "***";
        return masked + "@" + parts[1];
    }

    /** e.g. 0812345678 → 081***5678 */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 4);
    }
}