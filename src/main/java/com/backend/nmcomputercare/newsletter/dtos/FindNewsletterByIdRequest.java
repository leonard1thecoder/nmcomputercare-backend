package com.backend.nmcomputercare.newsletter.dtos;

import com.backend.nmcomputercare.utils.RequestContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ─────────────────────────────────────────────────────────────────────────────
// Find by ID
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Carries the primary key for a single-newsletter lookup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FindNewsletterByIdRequest implements RequestContract {
    private Long id;
}
