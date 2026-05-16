package com.backend.nmcomputercare.basiccare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persisted record for a Basic Care Plan service request.
 *
 * <p>A plan captures the exact combination of OS installation, driver upgrade,
 * and optional software the client has requested, together with a computed
 * total quote and the current lifecycle status.
 */
@Entity
@Table(name = "basic_care_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BasicCarePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Operating System ───────────────────────────────────────────────────

    /**
     * The operating system to install (or NO_OPERATING_SYSTEM if hardware-only).
     * Stored as the enum name; display order is governed by
     * {@link OperationSystem#getDisplayOrder()}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_system", nullable = false, length = 30)
    private OperationSystem operationSystem;

    // ── Driver Upgrade ─────────────────────────────────────────────────────

    /**
     * Whether a full driver refresh is included in this plan.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "upgrade_drivers", nullable = false, length = 10)
    private UpgradeDrivers upgradeDrivers;

    // ── Additional Software ────────────────────────────────────────────────

    /**
     * One optional performance/productivity software add-on.
     * Use {@link AdditionalPerformanceSoftware#NONE} when no software is wanted.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "additional_performance_software", nullable = false, length = 30)
    private AdditionalPerformanceSoftware additionalPerformanceSoftware;

    // ── Client Details ─────────────────────────────────────────────────────

    @Column(name = "issue_description", nullable = false, columnDefinition = "TEXT")
    private String issueDescription;

    /** Optional screenshot or diagnostic image path (relative to upload root). */
    @Column(name = "screenshot_file_path")
    private String screenShotFilePath;

    // ── Computed Quote ─────────────────────────────────────────────────────

    /**
     * Total quoted price, calculated as the sum of all selected component prices.
     * Stored for auditability — re-calculated on every save via {@link #computeQuote()}.
     */
    @Column(name = "total_quote", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalQuote;

    // ── Lifecycle ──────────────────────────────────────────────────────────

    /**
     * Current lifecycle status, stored as a numeric code.
     * <pre>0=CREATED, 1=MISSING_PAYMENT, 2=QUOTE_REJECTED, 3=PAID,
     *      4=WAITING_APPOINTMENT, 5=COMPLETED</pre>
     */
    @Column(name = "status", nullable = false)
    private byte status;

    @Column(name = "created_date",  nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    // ── JPA Hooks ──────────────────────────────────────────────────────────

    @PrePersist
    private void onPersist() {
        this.createdDate  = LocalDateTime.now();
        this.modifiedDate = LocalDateTime.now();
        this.status       = BasicCareStatus.CREATED.getCode();  // always 0 on creation
        computeQuote();
    }

    @PreUpdate
    private void onUpdate() {
        this.modifiedDate = LocalDateTime.now();
        computeQuote();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Recalculates {@link #totalQuote} from the three selected components.
     * Called automatically by JPA hooks so the stored value is always current.
     */
    public void computeQuote() {
        BigDecimal osPrice       = operationSystem             != null ? operationSystem.getPrice()             : BigDecimal.ZERO;
        BigDecimal driverPrice   = upgradeDrivers              != null ? upgradeDrivers.getPrice()              : BigDecimal.ZERO;
        BigDecimal softwarePrice = additionalPerformanceSoftware != null ? additionalPerformanceSoftware.getPrice() : BigDecimal.ZERO;
        this.totalQuote = osPrice.add(driverPrice).add(softwarePrice);
    }
}
