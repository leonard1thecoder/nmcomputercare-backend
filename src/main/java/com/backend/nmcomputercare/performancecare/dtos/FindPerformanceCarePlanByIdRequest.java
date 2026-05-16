package com.backend.nmcomputercare.performancecare.dtos;

import com.backend.nmcomputercare.utils.RequestContract;
import lombok.Builder;
import lombok.Data;


@Builder
@Data
public class FindPerformanceCarePlanByIdRequest implements RequestContract {

    private Long id;
}

