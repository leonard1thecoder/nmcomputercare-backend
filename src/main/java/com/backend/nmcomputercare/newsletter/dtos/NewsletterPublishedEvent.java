package com.backend.nmcomputercare.newsletter.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Spring application event published when a new newsletter is persisted.
 *
 * <p>The event listener queries all active subscribers and dispatches
 * an HTML email to each one.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterPublishedEvent {

    /** Primary key of the persisted newsletter (for audit / deduplication). */
    private Long   id;
    private String title;

    /**
     * Plain-text or HTML body — the mailer will embed the first
     * ~300 characters as a preview teaser inside the email template.
     */
    private String content;
}