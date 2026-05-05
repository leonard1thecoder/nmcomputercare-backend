package com.backend.nmcomputercare.newsletter.dtos;

import com.backend.nmcomputercare.utils.ResponseContract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Read-only view of a persisted {@code Newsletter}.
 * Implements {@link ResponseContract} so it integrates with the dispatcher pattern.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterResponse implements ResponseContract {
    private Long          id;
    private String        title;
    private String        content;
    private String        category;
    private LocalDateTime createdDate;
    private byte          status;
}
