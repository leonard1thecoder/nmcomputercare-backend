package com.backend.nmcomputercare.performancecare.entity;

import java.math.BigDecimal;

/**
 * Intel processor models available for upgrade, starting from Core i5.
 *
 * <p>Prices (ZAR, 2025) reflect current South African retail / OEM costs,
 * balanced for client affordability and business margin.  Always paired with
 * {@link CpuUpdate#INTEL}; set to {@code NONE} when {@link CpuUpdate} is
 * {@code NONE} or {@code AMD}.
 *
 * <table>
 *   <tr><th>Model</th><th>Gen</th><th>Cores/Threads</th><th>Price (ZAR)</th></tr>
 *   <tr><td>Core i5-10400F</td><td>10th</td><td>6C/12T</td><td>R 1 499</td></tr>
 *   <tr><td>Core i5-12400 </td><td>12th</td><td>6C/12T</td><td>R 2 299</td></tr>
 *   <tr><td>Core i5-13400 </td><td>13th</td><td>10C/16T</td><td>R 2 699</td></tr>
 *   <tr><td>Core i5-14400 </td><td>14th</td><td>10C/16T</td><td>R 3 099</td></tr>
 *   <tr><td>Core i7-12700 </td><td>12th</td><td>12C/20T</td><td>R 4 299</td></tr>
 *   <tr><td>Core i7-13700 </td><td>13th</td><td>16C/24T</td><td>R 4 999</td></tr>
 *   <tr><td>Core i7-14700 </td><td>14th</td><td>20C/28T</td><td>R 5 699</td></tr>
 *   <tr><td>Core i9-13900K</td><td>13th</td><td>24C/32T</td><td>R 8 299</td></tr>
 *   <tr><td>Core i9-14900K</td><td>14th</td><td>24C/32T</td><td>R 9 299</td></tr>
 * </table>
 */
public enum IntelModel {

    NONE(
            "No Intel CPU",
            BigDecimal.ZERO,
            "–", "–",
            "No Intel model selected."
    ),

    // ── Core i5 ─────────────────────────────────────────────────────────────
    CORE_I5_10400F(
            "Intel Core i5-10400F",
            new BigDecimal("1499.00"),
            "10th Gen", "6 Cores / 12 Threads",
            "Entry-level 10th-gen workhorse. No integrated graphics — requires a discrete GPU."
    ),
    CORE_I5_12400(
            "Intel Core i5-12400",
            new BigDecimal("2299.00"),
            "12th Gen (Alder Lake)", "6 Cores / 12 Threads",
            "Excellent price-to-performance. Ideal for everyday tasks and light gaming."
    ),
    CORE_I5_13400(
            "Intel Core i5-13400",
            new BigDecimal("2699.00"),
            "13th Gen (Raptor Lake)", "10 Cores / 16 Threads",
            "Hybrid architecture boosts multitasking. Strong mainstream choice."
    ),
    CORE_I5_14400(
            "Intel Core i5-14400",
            new BigDecimal("3099.00"),
            "14th Gen (Raptor Lake Refresh)", "10 Cores / 16 Threads",
            "Latest i5 refresh with improved turbo clocks."
    ),

    // ── Core i7 ─────────────────────────────────────────────────────────────
    CORE_I7_12700(
            "Intel Core i7-12700",
            new BigDecimal("4299.00"),
            "12th Gen (Alder Lake)", "12 Cores / 20 Threads",
            "Powerful 12-core chip for content creation and heavy multitasking."
    ),
    CORE_I7_13700(
            "Intel Core i7-13700",
            new BigDecimal("4999.00"),
            "13th Gen (Raptor Lake)", "16 Cores / 24 Threads",
            "16-core hybrid design. Handles demanding workloads and high-refresh gaming."
    ),
    CORE_I7_14700(
            "Intel Core i7-14700",
            new BigDecimal("5699.00"),
            "14th Gen (Raptor Lake Refresh)", "20 Cores / 28 Threads",
            "Top i7 offering. Excellent for video editing, 3D rendering and gaming."
    ),

    // ── Core i9 ─────────────────────────────────────────────────────────────
    CORE_I9_13900K(
            "Intel Core i9-13900K",
            new BigDecimal("8299.00"),
            "13th Gen (Raptor Lake)", "24 Cores / 32 Threads",
            "Flagship performance chip. Unlocked for overclocking."
    ),
    CORE_I9_14900K(
            "Intel Core i9-14900K",
            new BigDecimal("9299.00"),
            "14th Gen (Raptor Lake Refresh)", "24 Cores / 32 Threads",
            "Intel's peak consumer CPU. Maximum performance for professional workloads."
    );

    // ── Fields ──────────────────────────────────────────────────────────────
    private final String     displayName;
    private final BigDecimal price;
    private final String     generation;
    private final String     coreSpec;
    private final String     description;

    IntelModel(String displayName, BigDecimal price,
               String generation, String coreSpec, String description) {
        this.displayName = displayName;
        this.price       = price;
        this.generation  = generation;
        this.coreSpec    = coreSpec;
        this.description = description;
    }

    public String     getDisplayName() { return displayName; }
    public BigDecimal getPrice()       { return price;        }
    public String     getGeneration()  { return generation;   }
    public String     getCoreSpec()    { return coreSpec;     }
    public String     getDescription() { return description;  }
}
