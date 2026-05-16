package com.backend.nmcomputercare.basiccare.dtos;

import com.backend.nmcomputercare.basiccare.entity.AdditionalPerformanceSoftware;
import com.backend.nmcomputercare.basiccare.entity.BasicCareStatus;
import com.backend.nmcomputercare.basiccare.entity.OperationSystem;
import com.backend.nmcomputercare.basiccare.entity.UpgradeDrivers;
import com.backend.nmcomputercare.utils.ResponseContract;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only view of a persisted {@code BasicCarePlan}, returned by every
 * service operation.  Includes computed totals and human-readable labels
 * so the client never needs to look up enum metadata separately.
 */
@Data
@Builder
public class BasicCarePlanResponse implements ResponseContract {

    private Long   id;

    // ── Selected components ────────────────────────────────────────────────
    private OperationSystem               operationSystem;
    private String                        operationSystemDisplayName;
    private BigDecimal                    operationSystemPrice;

    private UpgradeDrivers                upgradeDrivers;
    private String                        upgradeDriversDisplayName;
    private BigDecimal                    upgradeDriversPrice;

    private AdditionalPerformanceSoftware additionalPerformanceSoftware;
    private String                        additionalPerformanceSoftwareDisplayName;
    private BigDecimal                    additionalPerformanceSoftwarePrice;

    // ── Client details ─────────────────────────────────────────────────────
    private String issueDescription;
    private String screenShotFilePath;

    // ── Quote ──────────────────────────────────────────────────────────────
    /** Sum of all selected component prices. */
    private BigDecimal totalQuote;

    // ── Lifecycle ──────────────────────────────────────────────────────────
    private byte            status;
    private String          statusDisplayName;
    private BasicCareStatus statusEnum;

    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}
