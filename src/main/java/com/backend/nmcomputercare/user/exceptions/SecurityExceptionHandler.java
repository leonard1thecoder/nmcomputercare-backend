package com.backend.nmcomputercare.user.exceptions;

import com.backend.nmcomputercare.utils.ExceptionHandlerReporter;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import com.backend.nmcomputercare.utils.exceptions.ServiceNameNotFoundException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised exception → HTTP response mapping for all security and user
 * module exceptions.  Scoped to the {@code user} and {@code security} packages.
 *
 * <p>Error envelope:
 * <pre>
 * {
 *   "timestamp":  "2025-06-01T10:15:30",
 *   "status":     403,
 *   "error":      "Forbidden",
 *   "message":    "Account is not yet verified.",
 *   "resolution": "Contact an administrator to activate your account."
 * }
 * </pre>
 */
@RestControllerAdvice(basePackages = {
        "com.backend.nmcomputercare.user",
        "com.backend.nmcomputercare.security"
})
public class SecurityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SecurityExceptionHandler.class);

    // ── 401 Unauthorized ────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        logger.warn("BadCredentialsException | {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(body(HttpStatus.UNAUTHORIZED,
                        "Invalid email address or password.",
                        "Check your credentials and try again."));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameNotFound(UsernameNotFoundException ex) {
        // Return generic message — do not confirm whether the account exists.
        logger.warn("UsernameNotFoundException | {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(body(HttpStatus.UNAUTHORIZED,
                        "Invalid email address or password.",
                        "Check your credentials and try again."));
    }

    // ── 403 Forbidden ───────────────────────────────────────────────────────

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException ex) {
        logger.warn("DisabledException | {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(body(HttpStatus.FORBIDDEN,
                        "Account is not yet verified.",
                        "Contact an administrator to activate your account."));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, Object>> handleLocked(LockedException ex) {
        logger.warn("LockedException | {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(body(HttpStatus.FORBIDDEN,
                        "Account is locked.",
                        "Contact an administrator."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        logger.warn("AccessDeniedException | {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(body(HttpStatus.FORBIDDEN,
                        "You do not have permission to access this resource.",
                        "Ensure your account has the required role."));
    }

    // ── 401 JWT errors ──────────────────────────────────────────────────────

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Map<String, Object>> handleExpiredJwt(ExpiredJwtException ex) {
        logger.warn("JWT expired | subject={}", ex.getClaims().getSubject());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(body(HttpStatus.UNAUTHORIZED,
                        "Your session has expired.",
                        "Please log in again to obtain a new token."));
    }

    @ExceptionHandler({MalformedJwtException.class, SignatureException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidJwt(Exception ex) {
        logger.warn("Invalid JWT | reason={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(body(HttpStatus.UNAUTHORIZED,
                        "Invalid or tampered authentication token.",
                        "Log in again to obtain a valid token."));
    }

    // ── 400 Domain exceptions ───────────────────────────────────────────────

    @ExceptionHandler(IncorrectRequestSentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IncorrectRequestSentException ex) {
        logger.warn("IncorrectRequestSentException | {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(body(HttpStatus.BAD_REQUEST, ex.getMessage(),
                        ExceptionHandlerReporter.getResolveIssueDetails()));
    }

    @ExceptionHandler(ServiceNameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleServiceNotFound(ServiceNameNotFoundException ex) {
        logger.error("ServiceNameNotFoundException | {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body(HttpStatus.NOT_FOUND, ex.getMessage(),
                        ExceptionHandlerReporter.getResolveIssueDetails()));
    }

    // ── 500 Catch-all ───────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        logger.error("Unhandled exception in security/user module", ex);
        return ResponseEntity.internalServerError()
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred.",
                        "Include the timestamp when contacting support."));
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    private Map<String, Object> body(HttpStatus status, String message, String resolution) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("timestamp",  LocalDateTime.now().toString());
        b.put("status",     status.value());
        b.put("error",      status.getReasonPhrase());
        b.put("message",    message);
        b.put("resolution", resolution != null ? resolution : "No additional details.");
        return b;
    }
}
