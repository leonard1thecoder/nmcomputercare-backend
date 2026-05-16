package com.backend.nmcomputercare.user.entity;

/**
 * Account verification status.
 *
 * <p>Newly registered users start as {@code NOT_VERIFIED}.
 * An administrator must set the status to {@code VERIFIED} before the
 * account becomes active.  Spring Security will reject login attempts
 * from {@code NOT_VERIFIED} accounts with a {@code 403 Forbidden}.
 */
public enum UserStatus {

    NOT_VERIFIED(
            "Not Verified",
            "Account registered but not yet verified by an administrator."
    ),
    VERIFIED(
            "Verified",
            "Account active and authorised to log in."
    );

    private final String displayName;
    private final String description;

    UserStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
