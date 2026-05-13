package com.backend.nmcomputercare.subscribe.service;

import com.backend.nmcomputercare.subscribe.dtos.VerificationRequest;
import com.backend.nmcomputercare.subscribe.dtos.VerificationResponse;
import com.backend.nmcomputercare.subscribe.entity.Subscription;
import com.backend.nmcomputercare.subscribe.entity.VerificationToken;
import com.backend.nmcomputercare.subscribe.exceptions.TokenAlreadyUsedException;
import com.backend.nmcomputercare.subscribe.exceptions.TokenExpiredException;
import com.backend.nmcomputercare.subscribe.exceptions.TokenNotFoundException;
import com.backend.nmcomputercare.subscribe.repository.SubscriptionRepository;
import com.backend.nmcomputercare.subscribe.repository.VerificationTokenRepository;
import com.backend.nmcomputercare.utils.ExceptionAdvice;
import com.backend.nmcomputercare.utils.ExecuteService;
import com.backend.nmcomputercare.utils.RequestContract;
import com.backend.nmcomputercare.utils.ResponseContract;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import com.backend.nmcomputercare.utils.exceptions.ServiceNameNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService implements ExecuteService {

    // ── Status constants (mirrors Subscription.status javadoc) ───────────────
    private static final byte STATUS_PENDING   = 0;
    private static final byte STATUS_CONFIRMED = 1;

    @Value("${app.verification.expiry-hours:24}")
    private int expiryHours;

    private final VerificationTokenRepository tokenRepository;
    private final SubscriptionRepository      subscriptionRepository;
    private final ExceptionAdvice             advice;

    @Transactional
    @Override
    public List<? extends ResponseContract> callable(String serviceName, RequestContract request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("serviceName", serviceName);

        try {
            log.info("Verification service invoked | service={} correlationId={}", serviceName, correlationId);

            return switch (serviceName) {
                case "verifySubscription" -> verifySubscription(request);
                default -> {
                    String msg = "Service name not found: " + serviceName;
                    throw advice.throwExceptionAndAdvice(
                            new ServiceNameNotFoundException(msg),
                            msg,
                            "Ensure the verification service name matches one of the defined cases.");
                }
            };
        } finally {
            MDC.clear();
        }
    }

    // ── Token issuance ────────────────────────────────────────────────────────

    /**
     * Generates (or regenerates) a verification token for {@code subscription}.
     * Any previous token for this subscriber is deleted first so only one
     * valid token exists at a time.
     *
     * @return the raw UUID string to embed in the verification URL
     */
    @Transactional
    public String createTokenFor(Subscription subscription) {
        // Remove stale tokens
        tokenRepository.deleteBySubscriptionId(subscription.getId());

        VerificationToken vt = VerificationToken.generate(subscription, expiryHours);
        tokenRepository.save(vt);

        log.info("Verification token issued for subscriber id={} email={}",
                subscription.getId(), subscription.getEmail());

        return vt.getToken();
    }

    // ── Verification ──────────────────────────────────────────────────────────

    /**
     * Validates the token and activates the subscription.
     *
     * @param rawToken the UUID string from the query parameter
     * @return the now-confirmed {@link Subscription}
     * @throws TokenNotFoundException   if no token matches
     * @throws TokenAlreadyUsedException if the token was already consumed
     * @throws TokenExpiredException    if the token window has passed
     */
    @Transactional
    public Subscription verify(String rawToken) {

        VerificationToken vt = tokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new TokenNotFoundException(
                        "Verification token not found: " + rawToken));

        if (vt.isUsed()) {
            throw new TokenAlreadyUsedException("This verification link has already been used.");
        }

        if (vt.isExpired()) {
            throw new TokenExpiredException(
                    "This verification link expired at " + vt.getExpiresAt() + ". Please request a new one.");
        }

        // Activate the subscription
        Subscription sub = vt.getSubscription();
        sub.setStatus(STATUS_CONFIRMED);
        sub.setActive(true);
        subscriptionRepository.save(sub);

        // Consume the token — mark used so it cannot be replayed
        vt.setUsed(true);
        tokenRepository.save(vt);

        log.info("Subscription confirmed: id={} email={}", sub.getId(), sub.getEmail());
        return sub;
    }

    @Transactional
    public List<VerificationResponse> verifySubscription(RequestContract request) {
        if (!(request instanceof VerificationRequest castedRequest)) {
            throw advice.throwExceptionAndAdvice(
                    new IncorrectRequestSentException("Request contract type mismatch."),
                    "Request contract type mismatch.",
                    "Pass a VerificationRequest with the token field populated.");
        }

        if (castedRequest.getToken() == null || castedRequest.getToken().isBlank()) {
            throw advice.throwExceptionAndAdvice(
                    new IncorrectRequestSentException("Verification token is required."),
                    "Verification token is required.",
                    "Open the verification link from the latest subscription email.");
        }

        return toResponseList(List.of(verify(castedRequest.getToken())));
    }

    // ── Token expiry value (used by email template) ───────────────────────────

    public int getExpiryHours() {
        return expiryHours;
    }

    private List<VerificationResponse> toResponseList(List<Subscription> subscriptions) {
        return subscriptions.stream()
                .map(subscription -> VerificationResponse.builder()
                        .id(subscription.getId())
                        .name(subscription.getName())
                        .email(subscription.getEmail())
                        .active(subscription.isActive())
                        .status(subscription.getStatus())
                        .build())
                .toList();
    }
}
