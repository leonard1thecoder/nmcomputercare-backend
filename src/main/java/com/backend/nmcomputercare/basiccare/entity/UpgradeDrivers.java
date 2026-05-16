package com.backend.nmcomputercare.basiccare.entity;

import java.math.BigDecimal;

/**
 * Whether the client requests a full driver update as part of the service.
 *
 * <p>Driver updates include fetching the latest chipset, GPU, audio, LAN/WLAN,
 * and peripheral drivers directly from each manufacturer's website — not
 * relying on Windows Update alone.
 *
 * <p>Pricing (ZAR):
 * <ul>
 *   <li>YES — R 129: reflects ~30 min technician time</li>
 *   <li>NO  — R   0: no driver work performed</li>
 * </ul>
 */
public enum UpgradeDrivers {

    YES(
            "Upgrade Drivers",
            new BigDecimal("129.00"),
            true,
            "All device drivers updated to latest manufacturer versions. " +
            "Improves stability, performance and hardware compatibility."
    ),
    NO(
            "No Driver Upgrade",
            BigDecimal.ZERO,
            false,
            "Existing drivers left as-is. Not recommended after a fresh OS install."
    );

    // ── Fields ─────────────────────────────────────────────────────────────

    private final String     displayName;
    private final BigDecimal price;
    private final boolean    requested;
    private final String     description;

    UpgradeDrivers(String displayName, BigDecimal price,
                   boolean requested, String description) {
        this.displayName = displayName;
        this.price       = price;
        this.requested   = requested;
        this.description = description;
    }

    public String     getDisplayName() { return displayName; }
    public BigDecimal getPrice()       { return price;        }
    public boolean    isRequested()    { return requested;    }
    public String     getDescription() { return description;  }
}
