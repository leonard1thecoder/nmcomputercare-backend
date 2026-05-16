package com.backend.nmcomputercare.basiccare.dtos;

import com.backend.nmcomputercare.basiccare.entity.AdditionalPerformanceSoftware;
import com.backend.nmcomputercare.basiccare.entity.OperationSystem;
import com.backend.nmcomputercare.basiccare.entity.UpgradeDrivers;
import com.backend.nmcomputercare.utils.RequestContract;
import lombok.Builder;
import lombok.Data;

/**
 * Inbound payload to update an existing {@code BasicCarePlan}.
 *
 * <p>All fields except {@code id} are optional — only non-null fields are
 * applied during the update, allowing partial patches without a dedicated
 * PATCH endpoint.
 */
@Data
@Builder
public class UpdateBasicCarePlanRequest implements RequestContract {

    /** Required — identifies the plan to update. */
    private Long id;

    private OperationSystem               operationSystem;
    private UpgradeDrivers                upgradeDrivers;
    private AdditionalPerformanceSoftware additionalPerformanceSoftware;

    private String issueDescription;
    private String screenShotFilePath;

    /**
     * Explicit status transition (numeric code).
     * If {@code null} the status is not changed.
     * Validate against {@link com.backend.nmcomputercare.basiccare.entity.BasicCareStatus}.
     */
    private Byte status;
}
