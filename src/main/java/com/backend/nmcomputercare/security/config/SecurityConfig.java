package com.backend.nmcomputercare.security.config;

import com.backend.nmcomputercare.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central Spring Security configuration.
 *
 * <h3>Endpoint access matrix</h3>
 * <pre>
 *  Rule (first match wins)                          Roles allowed
 *  ───────────────────────────────────────────────────────────────
 *  POST  /api/v1/auth/**                            PUBLIC
 *  GET   /api/v1/newsletters/**                     PUBLIC
 *  POST  /api/v1/subscriptions                      PUBLIC  (subscribe)
 *  GET   /api/subscriptions/verify                  PUBLIC  (email verify)
 *  POST  /api/v1/contact-forms                      PUBLIC  (submit enquiry)
 *
 *  POST/PUT/DELETE /api/v1/newsletters/**           ADMIN
 *  /api/v1/users/**                                 ADMIN
 *
 *  GET /api/v1/subscriptions/**                     ADMIN, KING_SPARKON_USER
 *  GET /api/v1/contact-forms/**                     ADMIN, KING_SPARKON_USER
 *  /api/v1/basic-care-plans/**                      ADMIN, KING_SPARKON_USER
 *  /api/v1/performance-care-plans/**                ADMIN, KING_SPARKON_USER
 *  /api/v1/business-care-plans/**                   ADMIN, KING_SPARKON_USER
 *
 *  Any other request                                authenticated
 * </pre>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    // ── Filter chain ──────────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)

            .authorizeHttpRequests(auth -> auth

                // ── Fully public ──────────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v1/newsletters/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/subscriptions/verify").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/subscriptions").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/contact-forms").permitAll()

                // ── ADMIN only ─────────────────────────────────────────────
                .requestMatchers(HttpMethod.POST,   "/api/v1/newsletters/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/newsletters/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/newsletters/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/users/**").hasRole("ADMIN")

                // ── ADMIN or KING_SPARKON_USER ─────────────────────────────
                .requestMatchers(HttpMethod.DELETE, "/api/v1/subscriptions").hasAnyRole("ADMIN", "KING_SPARKON_USER")
                .requestMatchers(HttpMethod.GET, "/api/v1/subscriptions/**").hasAnyRole("ADMIN", "KING_SPARKON_USER")
                .requestMatchers(HttpMethod.GET, "/api/v1/contact-forms/**").hasAnyRole("ADMIN", "KING_SPARKON_USER")
                .requestMatchers("/api/v1/basic-care-plans/**").hasAnyRole("ADMIN", "KING_SPARKON_USER")
                .requestMatchers("/api/v1/performance-care-plans/**").hasAnyRole("ADMIN", "KING_SPARKON_USER")
                .requestMatchers("/api/v1/business-care-plans/**").hasAnyRole("ADMIN", "KING_SPARKON_USER")

                // ── Everything else needs at least a valid token ───────────
                .anyRequest().authenticated()
            )

            // Stateless REST API — no HTTP session.
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authenticationProvider(authenticationProvider)

            // Run JWT filter before Spring's username/password filter.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ── Beans (defined here to avoid circular injection) ──────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder    passwordEncoder) {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
