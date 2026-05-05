package com.backend.nmcomputercare.subscribe.dtos;

import com.backend.nmcomputercare.utils.RequestContract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ─────────────────────────────────────────────────────────────────────────────
// Unsubscribe
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Request DTO for opting out of the newsletter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnsubscribeRequest implements RequestContract {

    private String email;
}
