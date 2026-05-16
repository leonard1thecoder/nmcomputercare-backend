package com.backend.nmcomputercare.basiccare.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Paginated envelope wrapping a list of {@link BasicCarePlanResponse} objects.
 * Mirrors {@code PagedSubscriptionResponse} for consistency across the API.
 */
@Data
@Builder
public class PagedBasicCarePlanResponse {

    private List<BasicCarePlanResponse> content;

    private int  page;
    private int  size;
    private long totalItems;
    private int  totalPages;
    private boolean first;
    private boolean last;
}
