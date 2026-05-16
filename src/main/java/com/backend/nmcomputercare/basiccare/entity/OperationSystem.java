package com.backend.nmcomputercare.basiccare.entity;

import java.math.BigDecimal;

/**
 * Supported operating systems available for installation.
 *
 * <p>Display order is intentional: Windows variants appear first (most common
 * client request), followed by Linux distributions, and finally the
 * "no OS" option.  The {@code displayOrder} field drives front-end sorting.
 *
 * <p>Pricing is in South African Rand (ZAR) and reflects current OEM /
 * retail licence costs as of 2025, balanced for client affordability and
 * business margin:
 * <ul>
 *   <li>Windows 11 Home OEM:  R 1 499</li>
 *   <li>Windows 10 Home OEM:  R 1 199 (still widely requested for legacy hw)</li>
 *   <li>Windows 8.1 OEM:      R   799 (legacy / refurb market)</li>
 *   <li>Linux distros:        R     0 (FOSS — labour only)</li>
 *   <li>No OS:                R     0</li>
 * </ul>
 */
public enum OperationSystem {

    // ── Windows (display first) ────────────────────────────────────────────
    WINDOWS_11(
            "Windows 11 Home",
            new BigDecimal("1499.00"),
            1,
            "Latest Microsoft OS. Requires TPM 2.0 & UEFI Secure Boot. " +
            "Includes DirectStorage, Auto HDR, and enhanced security features."
    ),
    WINDOWS_10(
            "Windows 10 Home",
            new BigDecimal("1199.00"),
            2,
            "Stable & widely supported. Ideal for hardware that does not meet " +
            "Windows 11 requirements. Support continues until October 2025."
    ),
    WINDOWS_8(
            "Windows 8.1",
            new BigDecimal("799.00"),
            3,
            "Legacy OS for older hardware. No longer receives security updates — " +
            "upgrade strongly recommended."
    ),

    // ── Linux (display second) ─────────────────────────────────────────────
    UBUNTU(
            "Ubuntu Linux (LTS)",
            BigDecimal.ZERO,
            4,
            "Most popular Linux distro. Rock-solid LTS release with 5-year " +
            "security support. Great for developers and general use."
    ),
    LINUX_MINT(
            "Linux Mint",
            BigDecimal.ZERO,
            5,
            "Beginner-friendly Linux based on Ubuntu. Familiar desktop layout " +
            "makes the transition from Windows easy."
    ),
    FEDORA(
            "Fedora Linux",
            BigDecimal.ZERO,
            6,
            "Cutting-edge open-source OS backed by Red Hat. Ideal for " +
            "developers wanting the latest packages."
    ),

    // ── No OS ──────────────────────────────────────────────────────────────
    NO_OPERATING_SYSTEM(
            "No Operating System",
            BigDecimal.ZERO,
            7,
            "Hardware service only. Client will source and install their own OS."
    );

    // ── Fields ─────────────────────────────────────────────────────────────

    private final String     displayName;
    private final BigDecimal price;
    private final int        displayOrder;
    private final String     description;

    OperationSystem(String displayName, BigDecimal price,
                    int displayOrder, String description) {
        this.displayName  = displayName;
        this.price        = price;
        this.displayOrder = displayOrder;
        this.description  = description;
    }

    public String     getDisplayName()  { return displayName;  }
    public BigDecimal getPrice()        { return price;         }
    public int        getDisplayOrder() { return displayOrder;  }
    public String     getDescription()  { return description;   }
}
