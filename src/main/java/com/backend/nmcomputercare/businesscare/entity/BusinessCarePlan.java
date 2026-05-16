package com.backend.nmcomputercare.businesscare.entity;

// ── BasicCare enums ───────────────────────────────────────────────────────────
import com.backend.nmcomputercare.basiccare.entity.AdditionalPerformanceSoftware;
import com.backend.nmcomputercare.basiccare.entity.ComputerType;
import com.backend.nmcomputercare.basiccare.entity.DisplayStatus;
import com.backend.nmcomputercare.basiccare.entity.OperationSystem;
import com.backend.nmcomputercare.basiccare.entity.UpgradeDrivers;

// ── PerformanceCare enums ─────────────────────────────────────────────────────
import com.backend.nmcomputercare.performancecare.entity.AmdModel;
import com.backend.nmcomputercare.performancecare.entity.CpuUpdate;
import com.backend.nmcomputercare.performancecare.entity.Ddr2Ram;
import com.backend.nmcomputercare.performancecare.entity.Ddr3Ram;
import com.backend.nmcomputercare.performancecare.entity.Ddr4Ram;
import com.backend.nmcomputercare.performancecare.entity.DeviceType;
import com.backend.nmcomputercare.performancecare.entity.IntelModel;
import com.backend.nmcomputercare.performancecare.entity.RamUpdate;
import com.backend.nmcomputercare.performancecare.entity.UpgradeGraphicCard;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persisted record for a <strong>Business Care Plan</strong> — a bulk service
 * offering that combines either the full Performance Care or the full Basic Care
 * service set, delivered across multiple devices in a single session.
 *
 * <h3>BulkType contract</h3>
 * <table>
 *   <tr><th>BulkType</th><th>Active pricing section</th><th>Inactive section</th></tr>
 *   <tr><td>{@code PERFORMANCE_CARE}</td>
 *       <td>CPU / RAM / GPU fields (single-price priority rule)<br>+ device labour premium</td>
 *       <td>All Basic Care fields stored as NONE, priced at R 0</td></tr>
 *   <tr><td>{@code BASIC_CARE}</td>
 *       <td>OS + drivers + additional software + display fields</td>
 *       <td>All Performance Care fields stored as NONE, priced at R 0</td></tr>
 * </table>
 *
 * <h3>Quote formula</h3>
 * <pre>
 *  unitPrice  =  [see BulkType section above]
 *  totalQuote =  unitPrice × quantity
 * </pre>
 *
 * <h3>Performance Care single-price rule</h3>
 * CPU, RAM, and GPU prices are <em>not</em> summed — only the highest-priority
 * selected upgrade price is charged (CPU → RAM → GPU order).
 */
@Entity
@Table(name = "business_care_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessCarePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Bulk metadata ──────────────────────────────────────────────────────

    /**
     * Whether this plan delivers Performance Care or Basic Care in bulk.
     * Drives which pricing section is active in {@link #computeQuote()}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "bulk_type", nullable = false, length = 20)
    private BulkType bulkType;

    /**
     * Number of devices included in this bulk service session.
     * Must be ≥ 1.  Defaults to 1 when not specified.
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // ═══════════════════════════════════════════════════════════════════════
    //  PERFORMANCE CARE fields
    //  Active when bulkType = PERFORMANCE_CARE; all NONE otherwise.
    // ═══════════════════════════════════════════════════════════════════════

    // ── CPU ────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "cpu_update", nullable = false, length = 10)
    private CpuUpdate cpuUpdate;

    @Enumerated(EnumType.STRING)
    @Column(name = "intel_model", nullable = false, length = 25)
    private IntelModel intelModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "amd_model", nullable = false, length = 20)
    private AmdModel amdModel;

    // ── RAM ────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "ram_update", nullable = false, length = 10)
    private RamUpdate ramUpdate;

    @Enumerated(EnumType.STRING)
    @Column(name = "ddr2_ram", nullable = false, length = 20)
    private Ddr2Ram ddr2Ram;

    @Enumerated(EnumType.STRING)
    @Column(name = "ddr3_ram", nullable = false, length = 20)
    private Ddr3Ram ddr3Ram;

    @Enumerated(EnumType.STRING)
    @Column(name = "ddr4_ram", nullable = false, length = 20)
    private Ddr4Ram ddr4Ram;

    // ── Device & GPU ───────────────────────────────────────────────────────

    /** Laptop or Desktop — carries a R 200 labour premium for laptops. */
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 15)
    private DeviceType deviceType;

    /**
     * GPU upgrade.  Must be {@link UpgradeGraphicCard#NONE} when
     * {@code deviceType = LAPTOP} or {@code bulkType = BASIC_CARE}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "upgrade_graphic_card", nullable = false, length = 20)
    private UpgradeGraphicCard upgradeGraphicCard;

    // ═══════════════════════════════════════════════════════════════════════
    //  BASIC CARE fields
    //  Active when bulkType = BASIC_CARE; all NONE / null otherwise.
    // ═══════════════════════════════════════════════════════════════════════

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_system", nullable = false, length = 30)
    private OperationSystem operationSystem;

    @Enumerated(EnumType.STRING)
    @Column(name = "upgrade_drivers", nullable = false, length = 10)
    private UpgradeDrivers upgradeDrivers;

    @Enumerated(EnumType.STRING)
    @Column(name = "additional_performance_software", nullable = false, length = 30)
    private AdditionalPerformanceSoftware additionalPerformanceSoftware;

    /** Required for Basic Care: client's description of the issue / service needed. */
    @Column(name = "issue_description", columnDefinition = "TEXT")
    private String issueDescription;

    /** Optional screenshot or diagnostic image path. */
    @Column(name = "screenshot_file_path")
    private String screenShotFilePath;

    /** Whether the client has a working display available (Basic Care). */
    @Enumerated(EnumType.STRING)
    @Column(name = "display_status", nullable = false, length = 10)
    private DisplayStatus displayStatus;

    /** Laptop or Desktop for Basic Care display-surcharge logic. */
    @Enumerated(EnumType.STRING)
    @Column(name = "computer_type", nullable = false, length = 15)
    private ComputerType computerType;

    // ═══════════════════════════════════════════════════════════════════════
    //  Computed quote & lifecycle
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Computed total: unitPrice × quantity.
     * Auto-recalculated on every persist/update.
     */
    @Column(name = "total_quote", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalQuote;

    @Column(name = "status", nullable = false)
    private byte status;

    @Column(name = "created_date",  nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    /**
     * Scheduled appointment date/time.
     * Populated when the plan transitions to {@link BusinessCareStatus#WAITING_APPOINTMENT}.
     */
    @Column(name = "booking_date")
    private LocalDateTime bookingDate;

    // ── JPA hooks ──────────────────────────────────────────────────────────

    @PrePersist
    private void onPersist() {
        this.createdDate  = LocalDateTime.now();
        this.modifiedDate = LocalDateTime.now();
        this.status       = BusinessCareStatus.CREATED.getCode();
        if (this.quantity == null || this.quantity < 1) this.quantity = 1;
        computeQuote();
    }

    @PreUpdate
    private void onUpdate() {
        this.modifiedDate = LocalDateTime.now();
        if (this.quantity == null || this.quantity < 1) this.quantity = 1;
        computeQuote();
    }

    // ── Quote calculation ──────────────────────────────────────────────────

    /**
     * Recomputes {@link #totalQuote} = unitPrice × {@link #quantity}.
     *
     * <h3>PERFORMANCE_CARE unit price</h3>
     * <pre>
     *  cpuPrice   = intelModel.price | amdModel.price | 0
     *  ramPrice   = ddr2/3/4Ram.price | 0
     *  gpuPrice   = upgradeGraphicCard.price | 0
     *
     *  upgradePrice = first non-zero of (cpuPrice, ramPrice, gpuPrice)
     *  unitPrice    = upgradePrice + deviceType.labourPremium
     * </pre>
     *
     * <h3>BASIC_CARE unit price</h3>
     * <pre>
     *  unitPrice = os + drivers + software + display
     *            + (R100 laptop surcharge if displayStatus=NO & computerType=LAPTOP)
     * </pre>
     */
    public void computeQuote() {
        BigDecimal unitPrice = (bulkType == BulkType.PERFORMANCE_CARE)
                ? computePerformanceUnitPrice()
                : computeBasicUnitPrice();

        int qty = (quantity != null && quantity > 0) ? quantity : 1;
        this.totalQuote = unitPrice.multiply(BigDecimal.valueOf(qty));
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private BigDecimal computePerformanceUnitPrice() {
        BigDecimal cpuPrice = resolveCpuPrice();
        BigDecimal ramPrice = resolveRamPrice();
        BigDecimal gpuPrice = (upgradeGraphicCard != null)
                ? upgradeGraphicCard.getPrice() : BigDecimal.ZERO;

        // Single-price rule: CPU → RAM → GPU priority.
        BigDecimal upgradePrice;
        if (cpuPrice.compareTo(BigDecimal.ZERO) > 0)       upgradePrice = cpuPrice;
        else if (ramPrice.compareTo(BigDecimal.ZERO) > 0)  upgradePrice = ramPrice;
        else                                                upgradePrice = gpuPrice;

        BigDecimal devicePremium = (deviceType != null)
                ? deviceType.getLabourPremium() : BigDecimal.ZERO;

        return upgradePrice.add(devicePremium);
    }

    private BigDecimal computeBasicUnitPrice() {
        BigDecimal osPrice       = (operationSystem != null)
                ? operationSystem.getPrice()               : BigDecimal.ZERO;
        BigDecimal driverPrice   = (upgradeDrivers != null)
                ? upgradeDrivers.getPrice()                : BigDecimal.ZERO;
        BigDecimal softwarePrice = (additionalPerformanceSoftware != null)
                ? additionalPerformanceSoftware.getPrice() : BigDecimal.ZERO;
        BigDecimal displayPrice  = (displayStatus != null)
                ? displayStatus.getPrice()                 : BigDecimal.ZERO;

        // R 100 surcharge when laptop has no display.
        BigDecimal laptopSurcharge = (displayStatus  == DisplayStatus.NO
                                   && computerType   == ComputerType.LAPTOP)
                ? new BigDecimal("100.00") : BigDecimal.ZERO;

        return osPrice.add(driverPrice).add(softwarePrice)
                      .add(displayPrice).add(laptopSurcharge);
    }

    private BigDecimal resolveCpuPrice() {
        if (cpuUpdate == null || cpuUpdate == CpuUpdate.NONE) return BigDecimal.ZERO;
        if (cpuUpdate == CpuUpdate.INTEL && intelModel != null) return intelModel.getPrice();
        if (cpuUpdate == CpuUpdate.AMD   && amdModel   != null) return amdModel.getPrice();
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveRamPrice() {
        if (ramUpdate == null || ramUpdate == RamUpdate.NONE) return BigDecimal.ZERO;
        return switch (ramUpdate) {
            case DDR2 -> (ddr2Ram != null ? ddr2Ram.getPrice() : BigDecimal.ZERO);
            case DDR3 -> (ddr3Ram != null ? ddr3Ram.getPrice() : BigDecimal.ZERO);
            case DDR4 -> (ddr4Ram != null ? ddr4Ram.getPrice() : BigDecimal.ZERO);
            default   -> BigDecimal.ZERO;
        };
    }
}
