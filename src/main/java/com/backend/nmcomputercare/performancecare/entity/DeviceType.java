package com.backend.nmcomputercare.performancecare.entity;

import java.math.BigDecimal;

/**
 * Physical form-factor of the device being upgraded.
 *
 * <p>Laptops carry a R 200 labour premium because component access requires
 * full disassembly, and upgrade options are more constrained (e.g. GPU upgrades
 * are generally not available on laptops and will be blocked in validation).
 *
 * <table>
 *   <tr><th>Type</th><th>Labour Premium (ZAR)</th></tr>
 *   <tr><td>LAPTOP</td><td>R 200</td></tr>
 *   <tr><td>DESKTOP</td><td>R 0</td></tr>
 * </table>
 */
public enum DeviceType {

    LAPTOP(
            "Laptop",
            new BigDecimal("200.00"),
            "Portable computer. Full disassembly required for component access. " +
            "R 200 labour premium applied. Note: GPU upgrades are not supported on laptops."
    ),
    DESKTOP(
            "Desktop Computer",
            BigDecimal.ZERO,
            "Tower or all-in-one desktop unit. Standard component access — no labour premium."
    );

    private final String     displayName;
    private final BigDecimal labourPremium;
    private final String     description;

    DeviceType(String displayName, BigDecimal labourPremium, String description) {
        this.displayName    = displayName;
        this.labourPremium  = labourPremium;
        this.description    = description;
    }

    public String     getDisplayName()   { return displayName;   }
    public BigDecimal getLabourPremium() { return labourPremium; }
    public String     getDescription()   { return description;   }
}
