package com.backend.nmcomputercare.subscribe.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Pagination envelope for subscription list responses.
 * Mirrors {@code PagedContactFormResponse} exactly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedSubscriptionResponse {
    private List<SubscriptionResponse> content;
    private int     page;
    private int     size;
    private long    totalItems;
    private int     totalPages;
    private boolean first;
    private boolean last;
}
