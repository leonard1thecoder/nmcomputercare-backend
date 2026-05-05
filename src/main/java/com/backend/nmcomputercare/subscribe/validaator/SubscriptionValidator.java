package com.backend.nmcomputercare.subscribe.validaator;

import com.backend.nmcomputercare.subscribe.dtos.SubscribeRequest;
import com.backend.nmcomputercare.utils.ExceptionAdvice;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Business-rule validation for subscription requests.
 * Basic null / format checks are handled by JSR-380 annotations on the DTO.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionValidator {

    private final ExceptionAdvice advice;

    public void validate(SubscribeRequest request) {

        if (request.getName() != null && request.getName().trim().length() < 2) {
            String msg = "Name must be at least 2 characters.";
            throw advice.throwExceptionAndAdvice(
                    new IncorrectRequestSentException(msg), msg,
                    "Provide a name with at least 2 characters.");
        }

        // Lightweight extra e-mail sanity check beyond @Email annotation.
        if (request.getEmail() != null && !request.getEmail().contains(".")) {
            String msg = "Email address appears to be invalid.";
            throw advice.throwExceptionAndAdvice(
                    new IncorrectRequestSentException(msg), msg,
                    "Ensure the email address contains a valid domain (e.g. user@example.com).");
        }
    }
}
