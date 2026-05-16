package com.backend.nmcomputercare.basiccare.dtos;

import com.backend.nmcomputercare.basiccare.entity.AdditionalPerformanceSoftware;
import com.backend.nmcomputercare.basiccare.entity.OperationSystem;
import com.backend.nmcomputercare.basiccare.entity.UpgradeDrivers;
import com.backend.nmcomputercare.utils.RequestContract;
import lombok.Builder;
import lombok.Data;

/**
 * Inbound payload to create a new {@code BasicCarePlan}.
 */
@Data
@Builder
public class CreateBasicCarePlanRequest implements RequestContract {

    private OperationSystem               operationSystem;
    private UpgradeDrivers                upgradeDrivers;
    private AdditionalPerformanceSoftware additionalPerformanceSoftware;

    /** Required: describe the fault or service needed. */
    private String issueDescription;

    /** Optional: relative path to an uploaded screenshot / diagnostic image. */
    private String screenShotFilePath;
}
