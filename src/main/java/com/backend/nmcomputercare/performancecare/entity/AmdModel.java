package com.backend.nmcomputercare.performancecare.entity;

import java.math.BigDecimal;

/**
 * AMD processor models available for upgrade, starting from Ryzen 5.
 *
 * <p>Prices (ZAR, 2025) reflect current South African retail / OEM costs.
 * Always paired with {@link CpuUpdate#AMD}; set to {@code NONE} when
 * {@link CpuUpdate} is {@code NONE} or {@code INTEL}.
 *
 * <table>
 *   <tr><th>Model</th><th>Series</th><th>Cores/Threads</th><th>Price (ZAR)</th></tr>
 *   <tr><td>Ryzen 5 5600  </td><td>Zen 3</td><td>6C/12T</td><td>R 1 599</td></tr>
 *   <tr><td>Ryzen 5 5600X </td><td>Zen 3</td><td>6C/12T</td><td>R 1 899</td></tr>
 *   <tr><td>Ryzen 5 7600  </td><td>Zen 4</td><td>6C/12T</td><td>R 2 499</td></tr>
 *   <tr><td>Ryzen 7 5700X </td><td>Zen 3</td><td>8C/16T</td><td>R 2 699</td></tr>
 *   <tr><td>Ryzen 7 7700  </td><td>Zen 4</td><td>8C/16T</td><td>R 3 799</td></tr>
 *   <tr><td>Ryzen 7 7700X </td><td>Zen 4</td><td>8C/16T</td><td>R 4 299</td></tr>
 *   <tr><td>Ryzen 9 5900X </td><td>Zen 3</td><td>12C/24T</td><td>R 5 299</td></tr>
 *   <tr><td>Ryzen 9 7900X </td><td>Zen 4</td><td>12C/24T</td><td>R 6 799</td></tr>
 *   <tr><td>Ryzen 9 7950X </td><td>Zen 4</td><td>16C/32T</td><td>R 9 799</td></tr>
 * </table>
 */
public enum AmdModel {

    NONE(
            "No AMD CPU",
            BigDecimal.ZERO,
            "–", "–",
            "No AMD model selected."
    ),

    // ── Ryzen 5 ─────────────────────────────────────────────────────────────
    RYZEN_5_5600(
            "AMD Ryzen 5 5600",
            new BigDecimal("1599.00"),
            "Zen 3 (5000 series)", "6 Cores / 12 Threads",
            "Best-value AMD chip for gaming and everyday use. PCIe 4.0 support."
    ),
    RYZEN_5_5600X(
            "AMD Ryzen 5 5600X",
            new BigDecimal("1899.00"),
            "Zen 3 (5000 series)", "6 Cores / 12 Threads",
            "Higher boost clocks than the 5600. Excellent 1080p gaming performance."
    ),
    RYZEN_5_7600(
            "AMD Ryzen 5 7600",
            new BigDecimal("2499.00"),
            "Zen 4 (7000 series)", "6 Cores / 12 Threads",
            "Latest Zen 4 architecture with DDR5 and PCIe 5.0. Future-proof platform."
    ),

    // ── Ryzen 7 ─────────────────────────────────────────────────────────────
    RYZEN_7_5700X(
            "AMD Ryzen 7 5700X",
            new BigDecimal("2699.00"),
            "Zen 3 (5000 series)", "8 Cores / 16 Threads",
            "8-core Zen 3 with strong multi-threaded performance at a competitive price."
    ),
    RYZEN_7_7700(
            "AMD Ryzen 7 7700",
            new BigDecimal("3799.00"),
            "Zen 4 (7000 series)", "8 Cores / 16 Threads",
            "Efficient 8-core Zen 4. Great for content creation and high-FPS gaming."
    ),
    RYZEN_7_7700X(
            "AMD Ryzen 7 7700X",
            new BigDecimal("4299.00"),
            "Zen 4 (7000 series)", "8 Cores / 16 Threads",
            "Unlocked 8-core Zen 4 with higher TDP for maximum boost performance."
    ),

    // ── Ryzen 9 ─────────────────────────────────────────────────────────────
    RYZEN_9_5900X(
            "AMD Ryzen 9 5900X",
            new BigDecimal("5299.00"),
            "Zen 3 (5000 series)", "12 Cores / 24 Threads",
            "12-core beast. Ideal for video editing, 3D work and streaming simultaneously."
    ),
    RYZEN_9_7900X(
            "AMD Ryzen 9 7900X",
            new BigDecimal("6799.00"),
            "Zen 4 (7000 series)", "12 Cores / 24 Threads",
            "Fastest 12-core consumer chip available. DDR5 + PCIe 5.0 platform."
    ),
    RYZEN_9_7950X(
            "AMD Ryzen 9 7950X",
            new BigDecimal("9799.00"),
            "Zen 4 (7000 series)", "16 Cores / 32 Threads",
            "AMD's flagship 16-core CPU. Workstation-class performance in a desktop chip."
    );

    // ── Fields ──────────────────────────────────────────────────────────────
    private final String     displayName;
    private final BigDecimal price;
    private final String     series;
    private final String     coreSpec;
    private final String     description;

    AmdModel(String displayName, BigDecimal price,
             String series, String coreSpec, String description) {
        this.displayName = displayName;
        this.price       = price;
        this.series      = series;
        this.coreSpec    = coreSpec;
        this.description = description;
    }

    public String     getDisplayName() { return displayName; }
    public BigDecimal getPrice()       { return price;        }
    public String     getSeries()      { return series;       }
    public String     getCoreSpec()    { return coreSpec;     }
    public String     getDescription() { return description;  }
}
