package com.backend.nmcomputercare.user.entity;

/**
 * User privilege levels.
 *
 * <p>Spring Security role names are derived by prepending {@code ROLE_}:
 * <ul>
 *   <li>{@code ADMIN}            → {@code ROLE_ADMIN}</li>
 *   <li>{@code KING_SPARKON_USER} → {@code ROLE_KING_SPARKON_USER}</li>
 * </ul>
 *
 * <p>Access matrix:
 * <pre>
 *  Endpoint group                       ADMIN   KING_SPARKON_USER   Public
 *  ─────────────────────────────────────────────────────────────────────────
 *  POST /auth/register, /auth/login       ✓           ✓              ✓
 *  GET  /newsletters/**                   ✓           ✓              ✓
 *  POST /subscriptions, /contact-forms    ✓           ✓              ✓
 *  GET  /subscriptions/verify             ✓           ✓              ✓
 *  GET  /contact-forms/**, /subscriptions ✓           ✓              ✗
 *  /basic-care-plans/**                   ✓           ✓              ✗
 *  /performance-care-plans/**             ✓           ✓              ✗
 *  /business-care-plans/**                ✓           ✓              ✗
 *  POST/PUT/DELETE /newsletters/**        ✓           ✗              ✗
 *  /users/**                              ✓           ✗              ✗
 * </pre>
 */
public enum Privilege {

    ADMIN(
            "Administrator",
            "Full access to all endpoints including user management, " +
            "newsletter creation, and all care plans."
    ),
    KING_SPARKON_USER(
            "KingSparkon User",
            "Access to care plans and read-only access to contact forms " +
            "and subscriptions. Cannot manage users or create newsletters."
    );

    private final String displayName;
    private final String description;

    Privilege(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
