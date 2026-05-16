package com.backend.nmcomputercare.businesscare.dtos;

import com.backend.nmcomputercare.basiccare.entity.AdditionalPerformanceSoftware;
import com.backend.nmcomputercare.basiccare.entity.ComputerType;
import com.backend.nmcomputercare.basiccare.entity.DisplayStatus;
import com.backend.nmcomputercare.basiccare.entity.OperationSystem;
import com.backend.nmcomputercare.basiccare.entity.UpgradeDrivers;
import com.backend.nmcomputercare.performancecare.entity.*;
import com.backend.nmcomputercare.businesscare.entity.BulkType;
import com.backend.nmcomputercare.utils.RequestContract;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Partial-update payload. Only non-null fields are applied to the persisted record.
 */
@Data
@Builder
public class UpdateBusinessCarePlanRequest implements RequestContract {

    /** Required — identifies the plan to update. */
    private Long id;

    private BulkType bulkType;
    private Integer  quantity;

    // Performance Care
    private CpuUpdate          cpuUpdate;
    private IntelModel         intelModel;
    private AmdModel           amdModel;
    private RamUpdate          ramUpdate;
    private Ddr2Ram            ddr2Ram;
    private Ddr3Ram            ddr3Ram;
    private Ddr4Ram            ddr4Ram;
    private DeviceType         deviceType;
    private UpgradeGraphicCard upgradeGraphicCard;

    // Basic Care
    private OperationSystem               operationSystem;
    private UpgradeDrivers                upgradeDrivers;
    private AdditionalPerformanceSoftware additionalPerformanceSoftware;
    private String                        issueDescription;
    private String                        screenShotFilePath;
    private DisplayStatus                 displayStatus;
    private ComputerType                  computerType;

    private LocalDateTime bookingDate;

    /**
     * Explicit status transition (0–5).
     * Null = no change.
     */
    private Byte status;
}
