package com.backend.nmcomputercare.user.controller;

import com.backend.nmcomputercare.user.dtos.AuthResponse;
import com.backend.nmcomputercare.user.dtos.LoginRequest;
import com.backend.nmcomputercare.user.dtos.RegisterRequest;
import com.backend.nmcomputercare.user.service.AuthService;
import com.backend.nmcomputercare.utils.ExceptionAdvice;
import com.backend.nmcomputercare.utils.ExecService;
import com.backend.nmcomputercare.utils.ResponseContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Public authentication endpoints.
 * Base path: {@code /api/v1/auth}
 *
 * <p>Both endpoints are permitted without a JWT token — configured in
 * {@link com.backend.nmcomputercare.security.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController extends ExecService {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService,
                          ExecutorService controllerExecutorService,
                          ExceptionAdvice advice) {
        super(controllerExecutorService, advice);
        this.authService = authService;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST  /api/v1/auth/register
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Register a new user account.
     *
     * <p>The new account starts with status {@code NOT_VERIFIED}.
     * An administrator must verify the account before login is permitted
     * on protected endpoints.
     *
     * <p>A JWT is returned immediately so the front-end can store it,
     * but it will be rejected on protected routes until verified.
     *
     * @return {@code 201 Created} with {@link AuthResponse} containing the JWT
     */
    @PostMapping("/register")
    public ResponseEntity<List<? extends ResponseContract>> register(
            @RequestBody RegisterRequest request) {

        logger.info("POST /auth/register | email={}", maskEmail(request.getEmailAddress()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exec(authService, "register", request));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  POST  /api/v1/auth/login
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Authenticate with email and password.
     *
     * <p>Returns a signed JWT on success.  Include it in subsequent requests as:
     * <pre>Authorization: Bearer &lt;token&gt;</pre>
     *
     * <p>Returns {@code 403 Forbidden} when the account has not been verified yet.
     *
     * @return {@code 200 OK} with {@link AuthResponse} containing the JWT
     */
    @PostMapping("/login")
    public ResponseEntity<List<? extends ResponseContract>> login(
            @RequestBody LoginRequest request) {

        logger.info("POST /auth/login | email={}", maskEmail(request.getEmailAddress()));
        return ResponseEntity.ok(exec(authService, "login", request));
    }

    // ── PII masking ────────────────────────────────────────────────────────

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] p = email.split("@");
        return (p[0].length() > 2 ? p[0].substring(0, 2) + "***" : "***") + "@" + p[1];
    }
}
