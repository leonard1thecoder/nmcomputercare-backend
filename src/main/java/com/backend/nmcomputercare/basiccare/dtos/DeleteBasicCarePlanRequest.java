package com.backend.nmcomputercare.basiccare.dtos;

import com.backend.nmcomputercare.utils.RequestContract;
import lombok.Builder;
import lombok.Data;

/** Request to hard-delete a plan record by its primary key. */
@Data
@Builder
public class DeleteBasicCarePlanRequest implements RequestContract {
    private Long id;
}
