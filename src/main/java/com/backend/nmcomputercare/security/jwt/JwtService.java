package com.backend.nmcomputercare.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Stateless JWT service: generates, validates, and parses signed JSON Web Tokens.
 *
 * <p>Required {@code application.yml} properties:
 * <pre>
 *  jwt:
 *    secret: &lt;Base64-encoded 256-bit key&gt;   # generate with: openssl rand -base64 32
 *    expiration: 86400000                      # 24 h in milliseconds
 * </pre>
 *
 * <p>Uses JJWT 0.12.x API (requires {@code jjwt-api}, {@code jjwt-impl},
 * {@code jjwt-jackson} on the classpath).
 */
@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    // ── Token generation ──────────────────────────────────────────────────

    /** Generate a token with no extra claims. */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /** Generate a token with additional custom claims (e.g. {@code role}). */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        logger.debug("Generating JWT | subject={}", userDetails.getUsername());
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Validation ────────────────────────────────────────────────────────

    /**
     * Returns {@code true} when the token subject matches the provided
     * {@link UserDetails} and the token has not expired.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        boolean valid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        if (!valid) logger.warn("JWT validation failed | subject={}", username);
        return valid;
    }

    // ── Claim extraction ──────────────────────────────────────────────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    // ── Internals ─────────────────────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
