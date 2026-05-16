package com.backend.nmcomputercare.performancecare.entity;

import java.math.BigDecimal;

/**
 * DDR3 RAM upgrade options.
 *
 * <p>Prices (ZAR, 2025) cover both new old-stock and quality refurbished kits.
 * Always paired with {@link RamUpdate#DDR3}.
 *
 * <table>
 *   <tr><th>Kit</th><th>Speed</th><th>Price (ZAR)</th></tr>
 *   <tr><td>4 GB</td><td>DDR3-1600</td><td>R 149</td></tr>
 *   <tr><td>8 GB</td><td>DDR3-1600</td><td>R 249</td></tr>
 *   <tr><td>16 GB (2×8 GB)</td><td>DDR3-1600</td><td>R 449</td></tr>
 *   <tr><td>32 GB (4×8 GB)</td><td>DDR3-1600</td><td>R 799</td></tr>
 * </table>
 */
public enum Ddr3Ram {

    NONE(
            "No DDR3 RAM",
            BigDecimal.ZERO,
            "No DDR3 kit selected."
    ),
    DDR3_4GB(
            "4 GB DDR3-1600",
            new BigDecimal("149.00"),
            "Single 4 GB stick. Basic upgrade for budget DDR3 systems."
    ),
    DDR3_8GB(
            "8 GB DDR3-1600",
            new BigDecimal("249.00"),
            "Single 8 GB stick or 2×4 GB dual-channel kit."
    ),
    DDR3_16GB_KIT(
            "16 GB DDR3-1600 Kit (2×8 GB)",
            new BigDecimal("449.00"),
            "Dual-channel 16 GB — sweet spot for DDR3 performance."
    ),
    DDR3_32GB_KIT(
            "32 GB DDR3-1600 Kit (4×8 GB)",
            new BigDecimal("799.00"),
            "Maximum DDR3 configuration. Ideal for workstations running legacy software."
    );

    private final String     displayName;
    private final BigDecimal price;
    private final String     description;

    Ddr3Ram(String displayName, BigDecimal price, String description) {
        this.displayName = displayName;
        this.price       = price;
        this.description = description;
    }

    public String     getDisplayName() { return displayName; }
    public BigDecimal getPrice()       { return price;        }
    public String     getDescription() { return description;  }
}
