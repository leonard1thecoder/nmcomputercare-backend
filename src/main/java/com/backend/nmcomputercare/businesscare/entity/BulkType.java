package com.backend.nmcomputercare.businesscare.entity;

/**
 * Indicates which service category is being offered as part of a bulk
 * {@link BusinessCarePlan}.
 *
 * <h3>Pricing implications</h3>
 * <ul>
 *   <li>{@code PERFORMANCE_CARE} — only hardware-upgrade pricing applies
 *       (CPU / RAM / GPU priority rule + device labour premium × quantity).
 *       All Basic Care fields default to NONE / R 0.</li>
 *   <li>{@code BASIC_CARE} — only software/OS pricing applies
 *       (OS + drivers + additional software + display fees × quantity).
 *       All Performance Care fields default to NONE / R 0.</li>
 * </ul>
 */
public enum BulkType {

    PERFORMANCE_CARE(
            "Performance Care (Bulk)",
            "Hardware upgrade service delivered to multiple devices in one session. " +
            "Includes CPU, RAM, and/or GPU upgrades. Basic Care fields are not priced."
    ),
    BASIC_CARE(
            "Basic Care (Bulk)",
            "Software and OS service delivered to multiple devices in one session. " +
            "Includes OS install, driver updates, and additional software. " +
            "Performance Care fields are not priced."
    );

    private final String displayName;
    private final String description;

    BulkType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
