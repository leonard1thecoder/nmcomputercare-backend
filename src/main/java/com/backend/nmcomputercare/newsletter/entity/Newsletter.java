package com.backend.nmcomputercare.newsletter.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a newsletter record persisted in the database.
 */
@Entity
@Table(name = "newsletters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Newsletter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short headline shown in email subject lines. */
    @Column(nullable = false, length = 255)
    private String title;

    /** Full HTML / plain-text body of the newsletter. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Timestamp the record was persisted. */
    @Column(nullable = false)
    private LocalDateTime createdDate;

    /**
     * 0 = draft, 1 = published, 2 = archived.
     * Stored as TINYINT to minimise storage.
     */
    @Column(nullable = false)
    private byte status;
}
