package com.backend.nmcomputercare.businesscare.dtos;

// BasicCare enums
import com.backend.nmcomputercare.basiccare.entity.AdditionalPerformanceSoftware;
import com.backend.nmcomputercare.basiccare.entity.ComputerType;
import com.backend.nmcomputercare.basiccare.entity.DisplayStatus;
import com.backend.nmcomputercare.basiccare.entity.OperationSystem;
import com.backend.nmcomputercare.basiccare.entity.UpgradeDrivers;
// PerformanceCare enums
import com.backend.nmcomputercare.performancecare.entity.*;
// BusinessCare enums
import com.backend.nmcomputercare.businesscare.entity.BulkType;
import com.backend.nmcomputercare.utils.RequestContract;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Inbound payload to create a new {@link com.backend.nmcomputercare.businesscare.entity.BusinessCarePlan}.
 *
 * <h3>BulkType contract</h3>
 * <ul>
 *   <li><b>PERFORMANCE_CARE</b>: populate all Performance Care fields; set all
 *       Basic Care fields to their NONE defaults.</li>
 *   <li><b>BASIC_CARE</b>: populate all Basic Care fields; set all
 *       Performance Care fields to their NONE defaults.</li>
 * </ul>
 * The validator enforces these rules and rejects cross-populated requests.
 */
@Data
@Builder
public class CreateBusinessCarePlanRequest implements RequestContract {

    // ── Bulk metadata ──────────────────────────────────────────────────────
    private BulkType bulkType;

    /** Number of devices in this session. Must be ≥ 1. */
    private Integer quantity;

    // ── Performance Care fields ────────────────────────────────────────────
    private CpuUpdate          cpuUpdate;
    private IntelModel         intelModel;
    private AmdModel           amdModel;
    private RamUpdate          ramUpdate;
    private Ddr2Ram            ddr2Ram;
    private Ddr3Ram            ddr3Ram;
    private Ddr4Ram            ddr4Ram;
    private DeviceType         deviceType;
    private UpgradeGraphicCard upgradeGraphicCard;

    // ── Basic Care fields ──────────────────────────────────────────────────
    private OperationSystem               operationSystem;
    private UpgradeDrivers                upgradeDrivers;
    private AdditionalPerformanceSoftware additionalPerformanceSoftware;
    private String                        issueDescription;
    private String                        screenShotFilePath;
    private DisplayStatus                 displayStatus;
    private ComputerType                  computerType;

    // ── Shared ────────────────────────────────────────────────────────────
    private LocalDateTime bookingDate;
}
