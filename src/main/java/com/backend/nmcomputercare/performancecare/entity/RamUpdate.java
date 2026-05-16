package com.backend.nmcomputercare.performancecare.entity;

/**
 * First-level RAM upgrade selector: which DDR generation (if any) to install.
 *
 * <p>No price is stored here — pricing is resolved from the generation-specific
 * capacity enum ({@link Ddr2Ram}, {@link Ddr3Ram}, or {@link Ddr4Ram}).
 *
 * <p>UI flow:
 * <pre>
 *  RamUpdate = NONE  → no RAM upgrade; all DDR enums set to NONE
 *  RamUpdate = DDR2  → client picks a kit from {@link Ddr2Ram}
 *  RamUpdate = DDR3  → client picks a kit from {@link Ddr3Ram}
 *  RamUpdate = DDR4  → client picks a kit from {@link Ddr4Ram}
 * </pre>
 */
public enum RamUpdate {

    NONE(
            "No RAM Upgrade",
            "Client does not require a RAM upgrade."
    ),
    DDR2(
            "DDR2 RAM",
            "Legacy DDR2 upgrade for older systems. Select capacity from Ddr2Ram."
    ),
    DDR3(
            "DDR3 RAM",
            "DDR3 upgrade for mid-generation boards. Select capacity from Ddr3Ram."
    ),
    DDR4(
            "DDR4 RAM",
            "Modern DDR4 upgrade for current-gen platforms. Select capacity from Ddr4Ram."
    );

    private final String displayName;
    private final String description;

    RamUpdate(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
