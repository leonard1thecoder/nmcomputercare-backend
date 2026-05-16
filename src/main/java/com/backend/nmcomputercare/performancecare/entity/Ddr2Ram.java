package com.backend.nmcomputercare.performancecare.entity;

import java.math.BigDecimal;

/**
 * DDR2 RAM upgrade options.
 *
 * <p>DDR2 is a legacy standard sourced from the refurbished / second-hand
 * market.  Prices (ZAR, 2025) reflect current South African used-hardware costs
 * plus a small handling margin.  Always paired with {@link RamUpdate#DDR2}.
 *
 * <table>
 *   <tr><th>Kit</th><th>Speed</th><th>Price (ZAR)</th></tr>
 *   <tr><td>2 GB</td><td>DDR2-800</td><td>R  89</td></tr>
 *   <tr><td>4 GB (2×2 GB)</td><td>DDR2-800</td><td>R 149</td></tr>
 *   <tr><td>8 GB (2×4 GB)</td><td>DDR2-800</td><td>R 249</td></tr>
 * </table>
 */
public enum Ddr2Ram {

    NONE(
            "No DDR2 RAM",
            BigDecimal.ZERO,
            "No DDR2 kit selected."
    ),
    DDR2_2GB(
            "2 GB DDR2-800",
            new BigDecimal("89.00"),
            "Single 2 GB DDR2 stick. Minimal upgrade for very old hardware."
    ),
    DDR2_4GB_KIT(
            "4 GB DDR2-800 Kit (2×2 GB)",
            new BigDecimal("149.00"),
            "Dual-channel 4 GB kit. Recommended minimum for DDR2 systems."
    ),
    DDR2_8GB_KIT(
            "8 GB DDR2-800 Kit (2×4 GB)",
            new BigDecimal("249.00"),
            "Maximum practical DDR2 configuration. Maximises performance on legacy boards."
    );

    private final String     displayName;
    private final BigDecimal price;
    private final String     description;

    Ddr2Ram(String displayName, BigDecimal price, String description) {
        this.displayName = displayName;
        this.price       = price;
        this.description = description;
    }

    public String     getDisplayName() { return displayName; }
    public BigDecimal getPrice()       { return price;        }
    public String     getDescription() { return description;  }
}
