package com.backend.nmcomputercare.performancecare.entity;

import java.math.BigDecimal;

/**
 * Graphics card upgrade options.
 *
 * <p>Prices (ZAR, 2025) reflect current South African retail costs for new
 * cards, balanced for client value and business margin.  Use {@code NONE} when
 * no GPU upgrade is required.
 *
 * <table>
 *   <tr><th>GPU</th><th>VRAM</th><th>Tier</th><th>Price (ZAR)</th></tr>
 *   <tr><td>GTX 1650</td><td>4 GB</td><td>Entry</td><td>R 1 999</td></tr>
 *   <tr><td>GTX 1660 Super</td><td>6 GB</td><td>Entry+</td><td>R 2 799</td></tr>
 *   <tr><td>RX 6600</td><td>8 GB</td><td>Mid</td><td>R 2 899</td></tr>
 *   <tr><td>RX 7600</td><td>8 GB</td><td>Mid</td><td>R 3 799</td></tr>
 *   <tr><td>RTX 3060</td><td>12 GB</td><td>Mid+</td><td>R 4 299</td></tr>
 *   <tr><td>RX 6700 XT</td><td>12 GB</td><td>Mid-high</td><td>R 4 799</td></tr>
 *   <tr><td>RTX 4060</td><td>8 GB</td><td>Mid-high</td><td>R 5 799</td></tr>
 *   <tr><td>RTX 3070</td><td>8 GB</td><td>High</td><td>R 6 299</td></tr>
 *   <tr><td>RTX 4070</td><td>12 GB</td><td>High</td><td>R 9 799</td></tr>
 * </table>
 */
public enum UpgradeGraphicCard {

    NONE(
            "No GPU Upgrade",
            BigDecimal.ZERO,
            "NVIDIA / AMD", "–",
            "No graphics card upgrade required."
    ),

    // ── Entry tier ──────────────────────────────────────────────────────────
    GTX_1650(
            "NVIDIA GeForce GTX 1650",
            new BigDecimal("1999.00"),
            "NVIDIA", "4 GB GDDR6",
            "Budget 1080p card. Low power draw (no external power connector needed)."
    ),
    GTX_1660_SUPER(
            "NVIDIA GeForce GTX 1660 Super",
            new BigDecimal("2799.00"),
            "NVIDIA", "6 GB GDDR6",
            "Step up from the 1650. Solid 1080p gaming without ray tracing."
    ),

    // ── Mid tier ────────────────────────────────────────────────────────────
    RX_6600(
            "AMD Radeon RX 6600",
            new BigDecimal("2899.00"),
            "AMD", "8 GB GDDR6",
            "AMD's best mid-range 1080p card. 8 GB VRAM future-proofs for upcoming titles."
    ),
    RX_7600(
            "AMD Radeon RX 7600",
            new BigDecimal("3799.00"),
            "AMD", "8 GB GDDR6",
            "RDNA 3 architecture. Excellent 1080p and capable 1440p performance."
    ),
    RTX_3060(
            "NVIDIA GeForce RTX 3060",
            new BigDecimal("4299.00"),
            "NVIDIA", "12 GB GDDR6",
            "Ray tracing and DLSS support. 12 GB VRAM is generous for this price range."
    ),

    // ── Mid-high tier ───────────────────────────────────────────────────────
    RX_6700_XT(
            "AMD Radeon RX 6700 XT",
            new BigDecimal("4799.00"),
            "AMD", "12 GB GDDR6",
            "Strong 1440p performer. FidelityFX Super Resolution boosts frame rates."
    ),
    RTX_4060(
            "NVIDIA GeForce RTX 4060",
            new BigDecimal("5799.00"),
            "NVIDIA", "8 GB GDDR6X",
            "Ada Lovelace architecture. DLSS 3 frame generation. Efficient 1080p/1440p card."
    ),

    // ── High tier ───────────────────────────────────────────────────────────
    RTX_3070(
            "NVIDIA GeForce RTX 3070",
            new BigDecimal("6299.00"),
            "NVIDIA", "8 GB GDDR6",
            "High-end 1440p and entry 4K gaming. Excellent ray tracing performance."
    ),
    RTX_4070(
            "NVIDIA GeForce RTX 4070",
            new BigDecimal("9799.00"),
            "NVIDIA", "12 GB GDDR6X",
            "Top-tier Ada Lovelace card. Outstanding 1440p/4K with DLSS 3 support."
    );

    // ── Fields ──────────────────────────────────────────────────────────────
    private final String     displayName;
    private final BigDecimal price;
    private final String     brand;
    private final String     vram;
    private final String     description;

    UpgradeGraphicCard(String displayName, BigDecimal price,
                       String brand, String vram, String description) {
        this.displayName = displayName;
        this.price       = price;
        this.brand       = brand;
        this.vram        = vram;
        this.description = description;
    }

    public String     getDisplayName() { return displayName; }
    public BigDecimal getPrice()       { return price;        }
    public String     getBrand()       { return brand;        }
    public String     getVram()        { return vram;         }
    public String     getDescription() { return description;  }
}
