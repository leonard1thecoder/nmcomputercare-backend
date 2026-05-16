package com.backend.nmcomputercare.businesscare.dtos;

import com.backend.nmcomputercare.basiccare.entity.AdditionalPerformanceSoftware;
import com.backend.nmcomputercare.basiccare.entity.ComputerType;
import com.backend.nmcomputercare.basiccare.entity.DisplayStatus;
import com.backend.nmcomputercare.basiccare.entity.OperationSystem;
import com.backend.nmcomputercare.basiccare.entity.UpgradeDrivers;
import com.backend.nmcomputercare.businesscare.entity.BulkType;
import com.backend.nmcomputercare.businesscare.entity.BusinessCareStatus;
import com.backend.nmcomputercare.performancecare.entity.*;
import com.backend.nmcomputercare.utils.ResponseContract;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only view of a {@link com.backend.nmcomputercare.businesscare.entity.BusinessCarePlan}.
 *
 * <p>Includes human-readable labels, per-component prices, and a transparent
 * quote breakdown so the front-end can render a detailed invoice line-by-line.
 */
@Data
@Builder
public class BusinessCarePlanResponse implements ResponseContract {

    private Long id;

    // ── Bulk metadata ──────────────────────────────────────────────────────
    private BulkType bulkType;
    private String   bulkTypeDisplayName;
    private Integer  quantity;

    // ═══════════════════════════════════════════════════════════════════════
    //  PERFORMANCE CARE section
    // ═══════════════════════════════════════════════════════════════════════

    private CpuUpdate  cpuUpdate;
    private IntelModel intelModel;
    private String     intelModelDisplayName;
    private BigDecimal intelModelPrice;
    private AmdModel   amdModel;
    private String     amdModelDisplayName;
    private BigDecimal amdModelPrice;
    /** Resolved CPU price (Intel or AMD, whichever was selected). */
    private BigDecimal resolvedCpuPrice;

    private RamUpdate  ramUpdate;
    private Ddr2Ram    ddr2Ram;
    private String     ddr2RamDisplayName;
    private BigDecimal ddr2RamPrice;
    private Ddr3Ram    ddr3Ram;
    private String     ddr3RamDisplayName;
    private BigDecimal ddr3RamPrice;
    private Ddr4Ram    ddr4Ram;
    private String     ddr4RamDisplayName;
    private BigDecimal ddr4RamPrice;
    /** Resolved RAM price (DDR2/3/4, whichever was selected). */
    private BigDecimal resolvedRamPrice;

    private DeviceType deviceType;
    private String     deviceTypeDisplayName;
    private BigDecimal deviceLabourPremium;

    private UpgradeGraphicCard upgradeGraphicCard;
    private String             upgradeGraphicCardDisplayName;
    private BigDecimal         upgradeGraphicCardPrice;

    /**
     * The single upgrade price applied under the CPU→RAM→GPU priority rule.
     * Only populated when {@code bulkType = PERFORMANCE_CARE}.
     */
    private BigDecimal appliedUpgradePrice;

    // ═══════════════════════════════════════════════════════════════════════
    //  BASIC CARE section
    // ═══════════════════════════════════════════════════════════════════════

    private OperationSystem operationSystem;
    private String          operationSystemDisplayName;
    private BigDecimal      operationSystemPrice;

    private UpgradeDrivers upgradeDrivers;
    private String         upgradeDriversDisplayName;
    private BigDecimal     upgradeDriversPrice;

    private AdditionalPerformanceSoftware additionalPerformanceSoftware;
    private String                        additionalPerformanceSoftwareDisplayName;
    private BigDecimal                    additionalPerformanceSoftwarePrice;

    private String issueDescription;
    private String screenShotFilePath;

    private DisplayStatus displayStatus;
    private String        displayStatusDisplayName;
    private BigDecimal    displayStatusPrice;

    private ComputerType computerType;
    private String       computerTypeDisplayName;
    /** R 100 surcharge applied when displayStatus=NO and computerType=LAPTOP. */
    private BigDecimal   laptopDisplaySurcharge;

    // ═══════════════════════════════════════════════════════════════════════
    //  Quote breakdown
    // ═══════════════════════════════════════════════════════════════════════

    /** Unit price (before quantity multiplier). */
    private BigDecimal unitPrice;
    /** unitPrice × quantity. */
    private BigDecimal totalQuote;

    // ── Lifecycle ─────────────────────────────────────────────────────────
    private byte               status;
    private BusinessCareStatus statusEnum;
    private String             statusDisplayName;

    // ── Timestamps ────────────────────────────────────────────────────────
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private LocalDateTime bookingDate;
}
