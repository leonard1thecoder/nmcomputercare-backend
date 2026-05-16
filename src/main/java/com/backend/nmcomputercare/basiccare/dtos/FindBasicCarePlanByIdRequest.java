package com.backend.nmcomputercare.basiccare.dtos;

import com.backend.nmcomputercare.utils.RequestContract;
import lombok.Builder;
import lombok.Data;

/** Lookup a single plan by its primary key. */
@Data
@Builder
public class FindBasicCarePlanByIdRequest implements RequestContract {
    private Long id;
}
