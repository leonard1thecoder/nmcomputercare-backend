package com.backend.nmcomputercare.subscribe.validaator;

import com.backend.nmcomputercare.subscribe.dtos.SubscribeRequest;
import com.backend.nmcomputercare.utils.ExceptionAdvice;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import com.backend.nmcomputercare.utils.*;
import java.time.LocalDateTime;
/**
 * Business-rule validation for subscription requests.
 * Basic null / format checks are handled by JSR-380 annotations on the DTO.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionValidator {

    private final ExceptionAdvice advice;

    public void validate(SubscribeRequest request) {

        // Lightweight extra e-mail sanity check beyond @Email annotation.
     if (request.getEmail() != null && !request.getEmail().contains(".")) {
    ExceptionHandlerReporter.setResolveIssueDetails("Ensure the email address contains a valid domain (e.g. user@example.com).");
    ExceptionHandlerReporter.setExceptionDate(LocalDateTime.now());
    throw new IncorrectRequestSentException("Email address appears to be invalid.");
}
    }
}
