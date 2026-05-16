package com.backend.nmcomputercare.performancecare.dtos;

import com.backend.nmcomputercare.utils.RequestContract;
import lombok.Builder;
import lombok.Data;

/** Hard-delete a plan by primary key. */
@Data
@Builder
public class DeletePerformanceCarePlanRequest implements RequestContract {
    private Long id;
}
