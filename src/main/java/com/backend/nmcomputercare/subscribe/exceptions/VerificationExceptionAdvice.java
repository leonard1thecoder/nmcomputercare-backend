package com.backend.nmcomputercare.subscribe.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised error handling for the subscription verification flow.
 *
 * <p>Every handler returns the same structured JSON body so the frontend
 * can handle all error cases uniformly:
 *
 * <pre>
 * {
 *   "timestamp" : "2026-05-12T10:30:00",
 *   "status"    : 404,
 *   "error"     : "NOT_FOUND",
 *   "code"      : "TOKEN_NOT_FOUND",
 *   "message"   : "...",
 *   "path"      : "/api/subscriptions/verify"
 * }
 * </pre>
 */
@Slf4j
@RestControllerAdvice
public class VerificationExceptionAdvice {

    // ── 404 NOT FOUND ─────────────────────────────────────────────────────────

    /**
     * Fired when the token does not exist in the database — either it was
     * never issued, or it was invalidated by a newer subscription attempt.
     */
    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTokenNotFound(
            TokenNotFoundException ex,
            WebRequest request) {

        log.warn("Token lookup failed — path={} message={}",
                extractPath(request), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildBody(
                        HttpStatus.NOT_FOUND,
                        "TOKEN_NOT_FOUND",
                        "Verification link not recognised. "
                        + "It may have been replaced by a newer request. "
                        + "Please check your inbox for the latest email.",
                        request));
    }

    // ── 409 CONFLICT ──────────────────────────────────────────────────────────

    /**
     * Fired when a subscriber tries to verify with a token that was already
     * consumed. Treated as a conflict — the resource is already in the correct
     * state, so no further action is needed.
     */
    @ExceptionHandler(TokenAlreadyUsedException.class)
    public ResponseEntity<Map<String, Object>> handleTokenAlreadyUsed(
            TokenAlreadyUsedException ex,
            WebRequest request) {

        log.info("Duplicate verification attempt — path={} message={}",
                extractPath(request), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildBody(
                        HttpStatus.CONFLICT,
                        "TOKEN_ALREADY_USED",
                        "Your email address has already been verified. "
                        + "You can close this page — no further action is needed.",
                        request));
    }

    // ── 410 GONE ──────────────────────────────────────────────────────────────

    /**
     * Fired when a valid token is found but its expiry window has passed.
     * HTTP 410 GONE signals that the resource existed but is permanently
     * unavailable — prompting the client to request a fresh link.
     */
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleTokenExpired(
            TokenExpiredException ex,
            WebRequest request) {

        log.warn("Expired token used — path={} message={}",
                extractPath(request), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.GONE)
                .body(buildBody(
                        HttpStatus.GONE,
                        "TOKEN_EXPIRED",
                        "This verification link has expired. "
                        + "Please subscribe again to receive a fresh confirmation email.",
                        request));
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    private Map<String, Object> buildBody(HttpStatus status,
                                          String code,
                                          String message,
                                          WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase().toUpperCase().replace(" ", "_"));
        body.put("code",      code);
        body.put("message",   message);
        body.put("path",      extractPath(request));
        return body;
    }

    private String extractPath(WebRequest request) {
        // WebRequest description format: "uri=/api/subscriptions/verify"
        return request.getDescription(false).replace("uri=", "");
    }
}
