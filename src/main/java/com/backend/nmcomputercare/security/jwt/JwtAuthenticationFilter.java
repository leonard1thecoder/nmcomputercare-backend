package com.backend.nmcomputercare.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts every request once, extracts the JWT from the
 * {@code Authorization: Bearer <token>} header, validates it, and
 * populates the {@link SecurityContextHolder} if the token is valid.
 *
 * <p>Requests without a valid Bearer token pass through the filter
 * unauthenticated — the {@link org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer}
 * in {@link com.backend.nmcomputercare.security.config.SecurityConfig}
 * then decides whether the endpoint is publicly accessible.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService         jwtService;
    private final UserDetailsService  userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest  request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain         filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // No Bearer token → pass through (public endpoints will still work).
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt      = authHeader.substring(BEARER_PREFIX.length());
        final String username;

        try {
            username = jwtService.extractUsername(jwt);
        } catch (Exception ex) {
            // Malformed / tampered token — let the request proceed unauthenticated.
            logger.warn("JWT extraction failed | reason={}", ex.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // Only authenticate if we have a username and no existing authentication.
        if (username != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.debug("JWT authenticated | user={} roles={}",
                        username, userDetails.getAuthorities());
            }
        }

        filterChain.doFilter(request, response);
    }
}
