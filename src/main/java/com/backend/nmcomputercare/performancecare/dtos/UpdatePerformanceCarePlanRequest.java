package com.backend.nmcomputercare.performancecare.dtos;

import com.backend.nmcomputercare.performancecare.entity.*;
import com.backend.nmcomputercare.utils.RequestContract;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Partial-update payload for an existing {@link com.backend.nmcomputercare.performancecare.entity.PerformanceCarePlan}.
 * Only non-null fields are applied; all others retain their current values.
 */
@Data
@Builder
public class UpdatePerformanceCarePlanRequest implements RequestContract {

    /** Required — identifies the plan to update. */
    private Long id;

    // CPU
    private CpuUpdate  cpuUpdate;
    private IntelModel intelModel;
    private AmdModel   amdModel;

    // RAM
    private RamUpdate ramUpdate;
    private Ddr2Ram   ddr2Ram;
    private Ddr3Ram   ddr3Ram;
    private Ddr4Ram   ddr4Ram;

    // Device & GPU
    private DeviceType         deviceType;
    private UpgradeGraphicCard upgradeGraphicCard;

    /** Set or update the appointment booking date/time. */
    private LocalDateTime bookingDate;

    /**
     * Explicit status transition (numeric code).
     * Null = no change.
     * Valid codes: 0-5 — see {@link PerformanceCareStatus}.
     */
    private Byte status;
}
