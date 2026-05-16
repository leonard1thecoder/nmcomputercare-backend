package com.backend.nmcomputercare.performancecare.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Paginated envelope wrapping a list of {@link PerformanceCarePlanResponse} objects. */
@Data
@Builder
public class PagedPerformanceCarePlanResponse {

    private List<PerformanceCarePlanResponse> content;

    private int     page;
    private int     size;
    private long    totalItems;
    private int     totalPages;
    private boolean first;
    private boolean last;
}
