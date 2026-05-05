package com.backend.nmcomputercare.subscribe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a newsletter subscriber persisted in the database.
 */
@Entity
@Table(
    name = "subscriptions",
    uniqueConstraints = @UniqueConstraint(columnNames = "email")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** Display name provided at subscription time. */
    @Column(length = 100)
    private String name;

    /** Timestamp when the subscriber first opted in. */
    @Column(nullable = false)
    private LocalDateTime subscribedDate;

    /**
     * Timestamp of the most recent unsubscribe action.
     * {@code null} when the subscription is still active.
     */
    @Column
    private LocalDateTime unsubscribedDate;

    /**
     * {@code true} = actively subscribed, {@code false} = unsubscribed.
     * Soft-delete pattern — rows are never physically removed.
     */
    @Column(nullable = false)
    private boolean active;

    /**
     * 0 = pending confirmation, 1 = confirmed, 2 = bounced.
     */
    @Column(nullable = false)
    private byte status;
}
