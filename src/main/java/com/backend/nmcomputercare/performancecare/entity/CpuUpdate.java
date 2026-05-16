package com.backend.nmcomputercare.performancecare.entity;

/**
 * First-level CPU upgrade selector: which CPU brand (if any) the client wants.
 *
 * <p>No price is stored here — pricing is resolved from the brand-specific
 * model enum ({@link IntelModel} or {@link AmdModel}) chosen in the next step.
 *
 * <p>UI flow:
 * <pre>
 *  CpuUpdate = NONE   → no CPU upgrade; IntelModel and AmdModel both set to NONE
 *  CpuUpdate = INTEL  → client picks a specific model from {@link IntelModel}
 *  CpuUpdate = AMD    → client picks a specific model from {@link AmdModel}
 * </pre>
 */
public enum CpuUpdate {

    NONE(
            "No CPU Upgrade",
            "Client does not require a CPU upgrade."
    ),
    INTEL(
            "Intel Processor",
            "Client wants an Intel CPU. Select the specific model from IntelModel."
    ),
    AMD(
            "AMD Processor",
            "Client wants an AMD CPU. Select the specific model from AmdModel."
    );

    private final String displayName;
    private final String description;

    CpuUpdate(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
