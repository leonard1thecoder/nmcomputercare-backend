package com.backend.nmcomputercare.user.validator;

import com.backend.nmcomputercare.user.dtos.LoginRequest;
import com.backend.nmcomputercare.user.dtos.RegisterRequest;
import com.backend.nmcomputercare.user.dtos.UpdateUserStatusRequest;
import com.backend.nmcomputercare.user.entity.UserStatus;
import com.backend.nmcomputercare.utils.ExceptionHandlerReporter;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Input validation for User and Auth requests.
 * Collects all violations before throwing.
 */
@Component
public class UserValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");
    private static final int MIN_PASSWORD_LEN = 8;

    // ── Register ───────────────────────────────────────────────────────────

    public void validateRegister(RegisterRequest req) {
        List<String> errors = new ArrayList<>();
        if (isBlank(req.getName()))         errors.add("name must not be blank.");
        if (isBlank(req.getSurname()))       errors.add("surname must not be blank.");
        if (isBlank(req.getEmailAddress()))  errors.add("emailAddress must not be blank.");
        else if (!EMAIL_PATTERN.matcher(req.getEmailAddress()).matches())
            errors.add("emailAddress '" + req.getEmailAddress() + "' is not a valid email.");
        if (isBlank(req.getPassword()))      errors.add("password must not be blank.");
        else if (req.getPassword().length() < MIN_PASSWORD_LEN)
            errors.add("password must be at least " + MIN_PASSWORD_LEN + " characters.");
        failIfErrors(errors);
    }

    // ── Login ──────────────────────────────────────────────────────────────

    public void validateLogin(LoginRequest req) {
        List<String> errors = new ArrayList<>();
        if (isBlank(req.getEmailAddress())) errors.add("emailAddress must not be blank.");
        if (isBlank(req.getPassword()))     errors.add("password must not be blank.");
        failIfErrors(errors);
    }

    // ── Update status ──────────────────────────────────────────────────────

    public void validateUpdateStatus(UpdateUserStatusRequest req) {
        List<String> errors = new ArrayList<>();
        if (req.getId() == null || req.getId() <= 0) errors.add("id must be a positive value.");
        if (req.getStatus() == null)                 errors.add("status must not be null.");
        failIfErrors(errors);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void failIfErrors(List<String> errors) {
        if (!errors.isEmpty()) {
            String detail = String.join(" | ", errors);
            ExceptionHandlerReporter.setResolveIssueDetails(detail);
            ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
            throw new IncorrectRequestSentException("Validation failed: " + detail);
        }
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}
