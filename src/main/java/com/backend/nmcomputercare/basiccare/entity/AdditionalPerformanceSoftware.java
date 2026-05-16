package com.backend.nmcomputercare.basiccare.entity;

import java.math.BigDecimal;

/**
 * Optional performance and productivity software add-ons.
 *
 * <p>Seven carefully selected titles covering the most requested categories:
 * security, privacy, productivity, maintenance, and system optimisation.
 * Pricing (ZAR) reflects the installation service fee plus the first-year
 * licence cost, balanced for client affordability and business margin.
 *
 * <table>
 *   <tr><th>#</th><th>Software</th><th>Category</th><th>Price (ZAR)</th></tr>
 *   <tr><td>1</td><td>Kaspersky Standard</td><td>Antivirus</td><td>R 399</td></tr>
 *   <tr><td>2</td><td>NordVPN</td><td>VPN / Privacy</td><td>R 299</td></tr>
 *   <tr><td>3</td><td>Microsoft 365 Personal</td><td>Productivity</td><td>R 899</td></tr>
 *   <tr><td>4</td><td>Malwarebytes Premium</td><td>Anti-Malware</td><td>R 299</td></tr>
 *   <tr><td>5</td><td>CCleaner Professional</td><td>System Cleaner</td><td>R 199</td></tr>
 *   <tr><td>6</td><td>IObit Driver Booster Pro</td><td>Driver Manager</td><td>R 149</td></tr>
 *   <tr><td>7</td><td>WinRAR</td><td>Archiver</td><td>R  99</td></tr>
 *   <tr><td>-</td><td>None</td><td>–</td><td>R    0</td></tr>
 * </table>
 */
public enum AdditionalPerformanceSoftware {

    /**
     * Kaspersky Standard — 1-year antivirus licence + installation.
     * Award-winning real-time threat protection, ransomware shield,
     * web filter and Safe Money module.
     * Price: R 399 (licence + setup).
     */
    KASPERSKY_STANDARD(
            "Kaspersky Standard Antivirus",
            "Antivirus / Security",
            new BigDecimal("399.00"),
            "Real-time malware & ransomware protection. Includes Safe Money " +
            "browser and web threat blocker. 1-year licence included."
    ),

    /**
     * NordVPN — 1-month subscription + installation.
     * Fastest consumer VPN. 6 simultaneous devices, no-log policy,
     * Threat Protection blocks ads and malicious sites.
     * Price: R 299 (1 month + setup).
     */
    NORD_VPN(
            "NordVPN",
            "VPN / Online Privacy",
            new BigDecimal("299.00"),
            "Encrypt your internet traffic and hide your IP. Blocks ads, " +
            "trackers, and malicious sites. 1-month subscription + setup."
    ),

    /**
     * Microsoft 365 Personal — 1-year subscription + installation.
     * Full Office suite (Word, Excel, PowerPoint, Outlook, OneNote),
     * 1 TB OneDrive storage, and advanced security features.
     * Price: R 899 (annual licence + setup).
     */
    MICROSOFT_365_PERSONAL(
            "Microsoft 365 Personal",
            "Productivity Suite",
            new BigDecimal("899.00"),
            "Word, Excel, PowerPoint, Outlook, OneNote + 1 TB OneDrive. " +
            "Always up-to-date Office apps for 1 PC/Mac. 1-year licence."
    ),

    /**
     * Malwarebytes Premium — 1-year licence + installation.
     * Industry-leading anti-malware scanner; pairs perfectly with
     * a traditional antivirus for layered defence.
     * Price: R 299.
     */
    MALWAREBYTES_PREMIUM(
            "Malwarebytes Premium",
            "Anti-Malware",
            new BigDecimal("299.00"),
            "Detects and removes malware, spyware, adware and PUPs that " +
            "traditional antivirus misses. Real-time protection. 1-year licence."
    ),

    /**
     * CCleaner Professional — 1-year licence + installation.
     * Cleans junk files, invalid registry entries, and browser traces.
     * Scheduled cleaning and real-time monitoring included.
     * Price: R 199.
     */
    CCLEANER_PROFESSIONAL(
            "CCleaner Professional",
            "System Cleaner / Optimiser",
            new BigDecimal("199.00"),
            "Clears junk files, browser history, and registry errors. " +
            "Automatic scheduled cleaning keeps the PC running smoothly. 1-year licence."
    ),

    /**
     * IObit Driver Booster Pro — 1-year licence + installation.
     * Auto-detects and updates all out-of-date drivers from a 8M+ database.
     * Ideal complement to the manual UpgradeDrivers service.
     * Price: R 149.
     */
    DRIVER_BOOSTER_PRO(
            "IObit Driver Booster Pro",
            "Driver Management",
            new BigDecimal("149.00"),
            "Automatically scans, downloads, and installs the latest drivers. " +
            "Backs up drivers before updating. 1-year licence."
    ),

    /**
     * WinRAR — lifetime licence + installation.
     * Industry-standard file archiver supporting RAR, ZIP, 7Z, TAR and more.
     * Price: R 99 (lifetime).
     */
    WINRAR(
            "WinRAR",
            "File Archiver",
            new BigDecimal("99.00"),
            "Create and extract RAR, ZIP, 7Z and many other archive formats. " +
            "Lifetime licence — no annual renewal required."
    ),

    /**
     * No additional software requested.
     */
    NONE(
            "No Additional Software",
            "None",
            BigDecimal.ZERO,
            "Client does not require any additional performance software."
    );

    // ── Fields ─────────────────────────────────────────────────────────────

    private final String     displayName;
    private final String     category;
    private final BigDecimal price;
    private final String     description;

    AdditionalPerformanceSoftware(String displayName, String category,
                                  BigDecimal price, String description) {
        this.displayName = displayName;
        this.category    = category;
        this.price       = price;
        this.description = description;
    }

    public String     getDisplayName() { return displayName; }
    public String     getCategory()    { return category;    }
    public BigDecimal getPrice()       { return price;        }
    public String     getDescription() { return description;  }
}
