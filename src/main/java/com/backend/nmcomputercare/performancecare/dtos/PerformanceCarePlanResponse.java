package com.backend.nmcomputercare.performancecare.dtos;

import com.backend.nmcomputercare.performancecare.entity.*;
import com.backend.nmcomputercare.utils.ResponseContract;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only view of a persisted {@link com.backend.nmcomputercare.performancecare.entity.PerformanceCarePlan}.
 *
 * <p>Includes human-readable display names and per-component prices so the
 * client never needs to look up enum metadata separately.  Also exposes the
 * applied upgrade price and the device labour premium separately for
 * transparent quote breakdowns on the front-end.
 */
@Data
@Builder
public class PerformanceCarePlanResponse implements ResponseContract {

    private Long id;

    // ── CPU ──────────────────────────────────────────────────────────────
    private CpuUpdate  cpuUpdate;
    private IntelModel intelModel;
    private String     intelModelDisplayName;
    private BigDecimal intelModelPrice;
    private AmdModel   amdModel;
    private String     amdModelDisplayName;
    private BigDecimal amdModelPrice;

    /**
     * The resolved CPU price that feeds into the upgrade-price priority logic.
     * Equals intelModelPrice or amdModelPrice depending on the brand chosen.
     */
    private BigDecimal resolvedCpuPrice;

    // ── RAM ──────────────────────────────────────────────────────────────
    private RamUpdate ramUpdate;
    private Ddr2Ram   ddr2Ram;
    private String    ddr2RamDisplayName;
    private BigDecimal ddr2RamPrice;
    private Ddr3Ram   ddr3Ram;
    private String    ddr3RamDisplayName;
    private BigDecimal ddr3RamPrice;
    private Ddr4Ram   ddr4Ram;
    private String    ddr4RamDisplayName;
    private BigDecimal ddr4RamPrice;

    /** The resolved RAM price that feeds into the upgrade-price priority logic. */
    private BigDecimal resolvedRamPrice;

    // ── Device & GPU ─────────────────────────────────────────────────────
    private DeviceType deviceType;
    private String     deviceTypeDisplayName;
    private BigDecimal deviceLabourPremium;

    private UpgradeGraphicCard upgradeGraphicCard;
    private String             upgradeGraphicCardDisplayName;
    private BigDecimal         upgradeGraphicCardPrice;

    // ── Quote Breakdown ───────────────────────────────────────────────────
    /**
     * The single upgrade price charged (CPU, RAM, or GPU — highest priority
     * non-zero, per business rule).
     */
    private BigDecimal appliedUpgradePrice;

    /** appliedUpgradePrice + deviceLabourPremium */
    private BigDecimal totalQuote;

    // ── Status ────────────────────────────────────────────────────────────
    private byte                   status;
    private PerformanceCareStatus  statusEnum;
    private String                 statusDisplayName;

    // ── Timestamps ───────────────────────────────────────────────────────
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private LocalDateTime bookingDate;
}
