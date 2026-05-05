package com.backend.nmcomputercare.newsletter.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Pagination envelope for newsletter list responses.
 * Mirrors {@code PagedContactFormResponse} exactly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedNewsletterResponse {
    private List<NewsletterResponse> content;
    private int     page;
    private int     size;
    private long    totalItems;
    private int     totalPages;
    private boolean first;
    private boolean last;
}
