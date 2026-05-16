package com.backend.nmcomputercare.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Application user — persisted account that drives Spring Security authentication.
 *
 * <p>The {@code emailAddress} field is the login credential (mapped to
 * {@link UserDetails#getUsername()}).
 *
 * <p>Account locking is tied to {@link UserStatus}:
 * <ul>
 *   <li>{@code NOT_VERIFIED} → {@link #isEnabled()} returns {@code false};
 *       Spring Security rejects login with {@code DisabledException}.</li>
 *   <li>{@code VERIFIED}     → fully active.</li>
 * </ul>
 */
@Entity
@Table(name = "users",
       uniqueConstraints = @UniqueConstraint(columnNames = "email_address"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "surname", nullable = false, length = 80)
    private String surname;

    /** BCrypt-hashed password — never stored or returned in plaintext. */
    @Column(name = "password", nullable = false)
    private String password;

    /** Used as the Spring Security {@code username} (login credential). */
    @Column(name = "email_address", nullable = false, unique = true, length = 150)
    private String emailAddress;

    @Column(name = "registered_date", nullable = false, updatable = false)
    private LocalDateTime registeredDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Privilege role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private UserStatus status;

    // ── JPA hook ──────────────────────────────────────────────────────────

    @PrePersist
    private void onPersist() {
        this.registeredDate = LocalDateTime.now();
        if (this.status == null)  this.status = UserStatus.NOT_VERIFIED;
    }

    // ── UserDetails ───────────────────────────────────────────────────────

    /**
     * Exposes the role as a Spring Security authority.
     * e.g. {@code ADMIN} → {@code ROLE_ADMIN}.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /** Login credential — the email address. */
    @Override
    public String getUsername() { return emailAddress; }

    @Override
    public boolean isAccountNonExpired()     { return true; }

    @Override
    public boolean isAccountNonLocked()      { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    /** Returns {@code false} (and blocks login) until an admin verifies the account. */
    @Override
    public boolean isEnabled() { return status == UserStatus.VERIFIED; }
}
