package com.backend.nmcomputercare.user.service;

import com.backend.nmcomputercare.security.jwt.JwtService;
import com.backend.nmcomputercare.user.dtos.*;
import com.backend.nmcomputercare.user.entity.Privilege;
import com.backend.nmcomputercare.user.entity.User;
import com.backend.nmcomputercare.user.entity.UserStatus;
import com.backend.nmcomputercare.user.repository.UserRepository;
import com.backend.nmcomputercare.user.validator.UserValidator;
import com.backend.nmcomputercare.utils.*;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import com.backend.nmcomputercare.utils.exceptions.ServiceNameNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles user registration and JWT login.
 *
 * <h3>Register flow</h3>
 * <ol>
 *   <li>Validate input.</li>
 *   <li>Check email uniqueness.</li>
 *   <li>BCrypt-hash the password.</li>
 *   <li>Persist with status {@code NOT_VERIFIED}.</li>
 *   <li>Return a JWT so the client can make an immediate follow-up call
 *       (token will be rejected on protected endpoints until an admin
 *       sets status to {@code VERIFIED}).</li>
 * </ol>
 *
 * <h3>Login flow</h3>
 * <ol>
 *   <li>Delegate to Spring's {@link AuthenticationManager} — this calls
 *       {@link com.backend.nmcomputercare.security.config.CustomUserDetailsService}
 *       and BCrypt-verifies the password.</li>
 *   <li>Throws {@code DisabledException} when account is NOT_VERIFIED
 *       (propagated as 403 by the exception handler).</li>
 *   <li>Return a signed JWT with {@code role} embedded as an extra claim.</li>
 * </ol>
 */
@RequiredArgsConstructor
@Data
@Service
@Transactional
public class AuthService implements ExecuteService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtService            jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserValidator         validator;

    // ══════════════════════════════════════════════════════════════════════
    //  Dispatcher
    // ══════════════════════════════════════════════════════════════════════

    @Override
    public List<? extends ResponseContract> callable(String serviceName, RequestContract request) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("serviceName",   serviceName);
        try {
            return switch (serviceName) {
                case "register" -> register(request);
                case "login"    -> login(request);
                default -> {
                    ExceptionHandlerReporter.setResolveIssueDetails("Valid names: register, login.");
                    ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
                    throw new ServiceNameNotFoundException("Service name not found: " + serviceName);
                }
            };
        } finally {
            MDC.clear();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Register
    // ══════════════════════════════════════════════════════════════════════

    private List<AuthResponse> register(RequestContract request) {
        if (!(request instanceof RegisterRequest req)) throw badRequest();
        validator.validateRegister(req);

        if (userRepository.existsByEmailAddress(req.getEmailAddress())) {
            ExceptionHandlerReporter.setResolveIssueDetails(
                    "Use a different email address or log in with the existing account.");
            ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
            throw new IncorrectRequestSentException(
                    "Email address already registered: " + req.getEmailAddress());
        }

        Privilege role = (req.getRole() != null) ? req.getRole() : Privilege.KING_SPARKON_USER;

        User user = User.builder()
                .name(req.getName())
                .surname(req.getSurname())
                .emailAddress(req.getEmailAddress())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .status(UserStatus.NOT_VERIFIED)
                .build();

        User saved = userRepository.save(user);
        logger.info("User registered | id={} email={} role={}",
                saved.getId(), maskEmail(saved.getEmailAddress()), saved.getRole());

        String token = jwtService.generateToken(buildExtraClaims(saved), saved);
        return List.of(toAuthResponse(saved, token));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Login
    // ══════════════════════════════════════════════════════════════════════

    private List<AuthResponse> login(RequestContract request) {
        if (!(request instanceof LoginRequest req)) throw badRequest();
        validator.validateLogin(req);

        // Spring Security validates credentials and throws on failure:
        // - BadCredentialsException  → wrong password
        // - DisabledException        → NOT_VERIFIED account
        // - UsernameNotFoundException → no such user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        req.getEmailAddress(), req.getPassword()));

        User user = userRepository.findByEmailAddress(req.getEmailAddress())
                .orElseThrow(() -> new IncorrectRequestSentException(
                        "User not found after authentication — this should not happen."));

        String token = jwtService.generateToken(buildExtraClaims(user), user);
        logger.info("User logged in | id={} email={} role={}",
                user.getId(), maskEmail(user.getEmailAddress()), user.getRole());

        return List.of(toAuthResponse(user, token));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════════

    private Map<String, Object> buildExtraClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role",   user.getRole().name());
        claims.put("userId", user.getId());
        return claims;
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .emailAddress(user.getEmailAddress())
                .role(user.getRole().name())
                .roleDisplayName(user.getRole().getDisplayName())
                .status(user.getStatus().name())
                .statusDisplayName(user.getStatus().getDisplayName())
                .registeredDate(user.getRegisteredDate())
                .build();
    }

    private RuntimeException badRequest() {
        ExceptionHandlerReporter.setResolveIssueDetails(
                "Pass a RegisterRequest for 'register', LoginRequest for 'login'.");
        ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
        return new IncorrectRequestSentException("Request contract type mismatch.");
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] p = email.split("@");
        return (p[0].length() > 2 ? p[0].substring(0, 2) + "***" : "***") + "@" + p[1];
    }
}
