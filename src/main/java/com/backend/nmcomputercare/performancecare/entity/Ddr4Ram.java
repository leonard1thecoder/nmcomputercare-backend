package com.backend.nmcomputercare.performancecare.entity;

import java.math.BigDecimal;

/**
 * DDR4 RAM upgrade options.
 *
 * <p>DDR4 is the current mainstream standard for most Intel 10th–14th gen and
 * AMD Ryzen 5000-series platforms.  Prices (ZAR, 2025) reflect new retail kits.
 * Always paired with {@link RamUpdate#DDR4}.
 *
 * <table>
 *   <tr><th>Kit</th><th>Speed</th><th>Price (ZAR)</th></tr>
 *   <tr><td>8 GB</td><td>DDR4-3200</td><td>R  299</td></tr>
 *   <tr><td>16 GB (2×8 GB)</td><td>DDR4-3200</td><td>R  499</td></tr>
 *   <tr><td>32 GB (2×16 GB)</td><td>DDR4-3200</td><td>R  899</td></tr>
 *   <tr><td>64 GB (2×32 GB)</td><td>DDR4-3200</td><td>R 1 599</td></tr>
 * </table>
 */
public enum Ddr4Ram {

    NONE(
            "No DDR4 RAM",
            BigDecimal.ZERO,
            "No DDR4 kit selected."
    ),
    DDR4_8GB(
            "8 GB DDR4-3200",
            new BigDecimal("299.00"),
            "Single 8 GB DDR4 stick. Entry-level upgrade for current-gen platforms."
    ),
    DDR4_16GB_KIT(
            "16 GB DDR4-3200 Kit (2×8 GB)",
            new BigDecimal("499.00"),
            "Dual-channel 16 GB — recommended for gaming and general productivity."
    ),
    DDR4_32GB_KIT(
            "32 GB DDR4-3200 Kit (2×16 GB)",
            new BigDecimal("899.00"),
            "32 GB dual-channel. Handles heavy multitasking, virtual machines and editing."
    ),
    DDR4_64GB_KIT(
            "64 GB DDR4-3200 Kit (2×32 GB)",
            new BigDecimal("1599.00"),
            "Maximum consumer DDR4. For professional workloads, large VMs and heavy rendering."
    );

    private final String     displayName;
    private final BigDecimal price;
    private final String     description;

    Ddr4Ram(String displayName, BigDecimal price, String description) {
        this.displayName = displayName;
        this.price       = price;
        this.description = description;
    }

    public String     getDisplayName() { return displayName; }
    public BigDecimal getPrice()       { return price;        }
    public String     getDescription() { return description;  }
}
