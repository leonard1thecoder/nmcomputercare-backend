package com.backend.nmcomputercare.performancecare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persisted record for a Performance Care Plan service request.
 *
 * <h3>Quote composition rule</h3>
 * CPU, RAM, and GPU upgrades each have a component price, but
 * <strong>only the highest-priority selected upgrade price is charged</strong>
 * — they are not summed together.  Priority order: CPU → RAM → GPU.
 *
 * <pre>
 *  upgradePrice = cpuPrice  (if CPU selected)
 *               | ramPrice  (else if RAM selected)
 *               | gpuPrice  (else if GPU selected)
 *               | R 0       (none selected)
 *
 *  totalQuote   = upgradePrice + deviceLabourPremium
 * </pre>
 *
 * This reflects a single technician session fee regardless of how many
 * components are swapped during that session.
 */
@Entity
@Table(name = "performance_care_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceCarePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── CPU ────────────────────────────────────────────────────────────────

    /** Brand selector: NONE / INTEL / AMD. */
    @Enumerated(EnumType.STRING)
    @Column(name = "cpu_update", nullable = false, length = 10)
    private CpuUpdate cpuUpdate;

    /**
     * Specific Intel model chosen.
     * Must be {@link IntelModel#NONE} when {@code cpuUpdate} is not {@link CpuUpdate#INTEL}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "intel_model", nullable = false, length = 25)
    private IntelModel intelModel;

    /**
     * Specific AMD model chosen.
     * Must be {@link AmdModel#NONE} when {@code cpuUpdate} is not {@link CpuUpdate#AMD}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "amd_model", nullable = false, length = 20)
    private AmdModel amdModel;

    // ── RAM ────────────────────────────────────────────────────────────────

    /** DDR generation selector: NONE / DDR2 / DDR3 / DDR4. */
    @Enumerated(EnumType.STRING)
    @Column(name = "ram_update", nullable = false, length = 10)
    private RamUpdate ramUpdate;

    /**
     * DDR2 capacity option.
     * Must be {@link Ddr2Ram#NONE} when {@code ramUpdate} is not {@link RamUpdate#DDR2}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ddr2_ram", nullable = false, length = 20)
    private Ddr2Ram ddr2Ram;

    /**
     * DDR3 capacity option.
     * Must be {@link Ddr3Ram#NONE} when {@code ramUpdate} is not {@link RamUpdate#DDR3}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ddr3_ram", nullable = false, length = 20)
    private Ddr3Ram ddr3Ram;

    /**
     * DDR4 capacity option.
     * Must be {@link Ddr4Ram#NONE} when {@code ramUpdate} is not {@link RamUpdate#DDR4}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ddr4_ram", nullable = false, length = 20)
    private Ddr4Ram ddr4Ram;

    // ── Device Type ────────────────────────────────────────────────────────

    /**
     * Laptop or Desktop.  Laptops carry a R 200 labour premium and cannot
     * receive a GPU upgrade (enforced by {@link com.backend.nmcomputercare.performancecare.validator.PerformanceCarePlanValidator}).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 15)
    private DeviceType deviceType;

    // ── GPU ────────────────────────────────────────────────────────────────

    /**
     * Discrete graphics card upgrade.
     * Use {@link UpgradeGraphicCard#NONE} when no GPU upgrade is required.
     * GPU upgrades are not permitted when {@code deviceType} is {@link DeviceType#LAPTOP}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "upgrade_graphic_card", nullable = false, length = 20)
    private UpgradeGraphicCard upgradeGraphicCard;

    // ── Timestamps ─────────────────────────────────────────────────────────

    @Column(name = "created_date",  nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    /**
     * Date and time the client's technician appointment is scheduled.
     * Populated when the plan transitions to {@link PerformanceCareStatus#WAITING_APPOINTMENT}.
     */
    @Column(name = "booking_date")
    private LocalDateTime bookingDate;

    // ── Computed Quote ─────────────────────────────────────────────────────

    /**
     * Total quoted price.  Recomputed automatically on every persist/update.
     * See class-level Javadoc for the pricing rule.
     */
    @Column(name = "total_quote", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalQuote;

    // ── Lifecycle ──────────────────────────────────────────────────────────

    /**
     * Current lifecycle status code.
     * <pre>0=CREATED, 1=MISSING_PAYMENT, 2=QUOTE_REJECTED,
     *      3=PAID, 4=WAITING_APPOINTMENT, 5=COMPLETED</pre>
     */
    @Column(name = "status", nullable = false)
    private byte status;

    // ── JPA Hooks ──────────────────────────────────────────────────────────

    @PrePersist
    private void onPersist() {
        this.createdDate  = LocalDateTime.now();
        this.modifiedDate = LocalDateTime.now();
        this.status       = PerformanceCareStatus.CREATED.getCode();
        computeQuote();
    }

    @PreUpdate
    private void onUpdate() {
        this.modifiedDate = LocalDateTime.now();
        computeQuote();
    }

    // ── Quote Calculation ──────────────────────────────────────────────────

    /**
     * Recalculates {@link #totalQuote}.
     *
     * <p><strong>Upgrade pricing rule:</strong> only ONE upgrade price applies
     * across CPU, RAM, and GPU — the first (highest-priority) non-zero price in
     * the order CPU → RAM → GPU.  This represents a single technician session fee.
     *
     * <pre>
     *  cpuPrice  = intelModel.price  (if cpuUpdate = INTEL)
     *            | amdModel.price    (if cpuUpdate = AMD)
     *            | 0
     *
     *  ramPrice  = ddr2Ram.price     (if ramUpdate = DDR2)
     *            | ddr3Ram.price     (if ramUpdate = DDR3)
     *            | ddr4Ram.price     (if ramUpdate = DDR4)
     *            | 0
     *
     *  gpuPrice  = upgradeGraphicCard.price
     *
     *  upgradePrice = first non-zero of (cpuPrice, ramPrice, gpuPrice)
     *  totalQuote   = upgradePrice + deviceType.labourPremium
     * </pre>
     */
    public void computeQuote() {
        BigDecimal cpuPrice = resolveCpuPrice();
        BigDecimal ramPrice = resolveRamPrice();
        BigDecimal gpuPrice = (upgradeGraphicCard != null)
                ? upgradeGraphicCard.getPrice()
                : BigDecimal.ZERO;

        // Priority cascade: CPU first, then RAM, then GPU.
        BigDecimal upgradePrice;
        if (cpuPrice.compareTo(BigDecimal.ZERO) > 0) {
            upgradePrice = cpuPrice;
        } else if (ramPrice.compareTo(BigDecimal.ZERO) > 0) {
            upgradePrice = ramPrice;
        } else {
            upgradePrice = gpuPrice;
        }

        BigDecimal devicePremium = (deviceType != null)
                ? deviceType.getLabourPremium()
                : BigDecimal.ZERO;

        this.totalQuote = upgradePrice.add(devicePremium);
    }

    // ── Private helpers ────────────────────────────────────────────────────

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
