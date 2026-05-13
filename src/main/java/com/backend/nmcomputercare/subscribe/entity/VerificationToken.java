package com.backend.nmcomputercare.subscribe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One-time token used to confirm a subscriber's email address.
 * A new token is issued on every subscription attempt; old tokens
 * for the same subscriber are invalidated when a fresh one is saved.
 */
@Entity
@Table(name = "verification_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * URL-safe UUID token sent inside the verification link.
     */
    @Column(nullable = false, unique = true, length = 36)
    private String token;

    /**
     * The subscriber this token belongs to.
     * Cascade REMOVE ensures tokens are cleaned up if a subscription is deleted.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    /**
     * When this token was issued.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Hard deadline — token cannot be used after this point.
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * {@code true} once the subscriber has successfully clicked the link.
     * Prevents replay attacks.
     */
    @Column(nullable = false)
    private boolean used;

    // ── Factory helper ────────────────────────────────────────────────────────

    /**
     * Creates a fresh, unused token that expires after {@code expiryHours} hours.
     */
    public static VerificationToken generate(Subscription subscription, int expiryHours) {
        LocalDateTime now = LocalDateTime.now();
        return VerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .subscription(subscription)
                .createdAt(now)
                .expiresAt(now.plusHours(expiryHours))
                .used(false)
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}
