package com.backend.nmcomputercare.basiccare.validator;

import com.backend.nmcomputercare.basiccare.dtos.CreateBasicCarePlanRequest;
import com.backend.nmcomputercare.basiccare.dtos.UpdateBasicCarePlanRequest;
import com.backend.nmcomputercare.basiccare.entity.BasicCareStatus;
import com.backend.nmcomputercare.utils.ExceptionHandlerReporter;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates incoming Basic Care Plan requests before they reach the service layer.
 *
 * <p>All rule violations are collected before throwing so the client receives
 * a complete list of problems in a single response.
 */
@Component
public class BasicCarePlanValidator {

    private static final int MAX_DESCRIPTION_LENGTH = 2_000;
    private static final int MAX_PATH_LENGTH        = 500;

    // ── Create ─────────────────────────────────────────────────────────────

    /**
     * Validates a {@link CreateBasicCarePlanRequest}.
     *
     * @throws IncorrectRequestSentException if any rule is violated
     */
    public void validateCreate(CreateBasicCarePlanRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.getOperationSystem() == null) {
            errors.add("operationSystem must not be null.");
        }
        if (request.getUpgradeDrivers() == null) {
            errors.add("upgradeDrivers must not be null.");
        }
        if (request.getAdditionalPerformanceSoftware() == null) {
            errors.add("additionalPerformanceSoftware must not be null. Use NONE if not required.");
        }
        if (isBlank(request.getIssueDescription())) {
            errors.add("issueDescription must not be blank.");
        } else if (request.getIssueDescription().length() > MAX_DESCRIPTION_LENGTH) {
            errors.add("issueDescription must not exceed " + MAX_DESCRIPTION_LENGTH + " characters.");
        }
        if (request.getScreenShotFilePath() != null
                && request.getScreenShotFilePath().length() > MAX_PATH_LENGTH) {
            errors.add("screenShotFilePath must not exceed " + MAX_PATH_LENGTH + " characters.");
        }

        failIfErrors(errors);
    }

    // ── Update ─────────────────────────────────────────────────────────────

    /**
     * Validates an {@link UpdateBasicCarePlanRequest}.
     *
     * @throws IncorrectRequestSentException if any rule is violated
     */
    public void validateUpdate(UpdateBasicCarePlanRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.getId() == null || request.getId() <= 0) {
            errors.add("id must be a positive non-null value.");
        }
        if (request.getIssueDescription() != null) {
            if (isBlank(request.getIssueDescription())) {
                errors.add("issueDescription must not be blank when provided.");
            } else if (request.getIssueDescription().length() > MAX_DESCRIPTION_LENGTH) {
                errors.add("issueDescription must not exceed " + MAX_DESCRIPTION_LENGTH + " characters.");
            }
        }
        if (request.getScreenShotFilePath() != null
                && request.getScreenShotFilePath().length() > MAX_PATH_LENGTH) {
            errors.add("screenShotFilePath must not exceed " + MAX_PATH_LENGTH + " characters.");
        }
        if (request.getStatus() != null) {
            try {
                BasicCareStatus.fromCode(request.getStatus());
            } catch (IllegalArgumentException e) {
                errors.add("status code " + request.getStatus() + " is invalid. "
                        + "Valid codes: 0=CREATED, 1=MISSING_PAYMENT, 2=QUOTE_REJECTED, "
                        + "3=PAID, 4=WAITING_APPOINTMENT, 5=COMPLETED.");
            }
        }

        failIfErrors(errors);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void failIfErrors(List<String> errors) {
        if (!errors.isEmpty()) {
            String detail = String.join(" | ", errors);
            ExceptionHandlerReporter.setResolveIssueDetails(detail);
            ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
            throw new IncorrectRequestSentException(
                    "Validation failed for BasicCarePlan request: " + detail);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
