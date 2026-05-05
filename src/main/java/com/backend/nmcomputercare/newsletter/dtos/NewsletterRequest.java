package com.backend.nmcomputercare.newsletter.dtos;


import com.backend.nmcomputercare.utils.RequestContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ─────────────────────────────────────────────────────────────────────────────
// Create newsletter
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Request DTO for creating a new newsletter draft.
 * Implements {@link RequestContract} so it can flow through the dispatcher.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterRequest implements RequestContract {

    private String title;

    private String content;

}
