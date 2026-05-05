package com.backend.nmcomputercare.newsletter.validaator;

import com.backend.nmcomputercare.newsletter.dtos.NewsletterRequest;
import com.backend.nmcomputercare.utils.ExceptionAdvice;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates a {@link NewsletterRequest} before any persistence occurs.
 * Field-level JSR-380 annotations on the DTO handle basic null / blank checks;
 * this class adds cross-field and business-rule validation.
 */
@Component
@RequiredArgsConstructor
public class NewsletterValidator {

    private final ExceptionAdvice advice;

    public void validate(NewsletterRequest request) {

        if (request.getTitle() != null && request.getTitle().trim().length() < 3) {
            String msg = "Newsletter title must be at least 3 characters.";
            throw advice.throwExceptionAndAdvice(
                    new IncorrectRequestSentException(msg), msg,
                    "Provide a meaningful title with at least 3 characters.");
        }

        if (request.getContent() != null && request.getContent().trim().length() < 20) {
            String msg = "Newsletter content must be at least 20 characters.";
            throw advice.throwExceptionAndAdvice(
                    new IncorrectRequestSentException(msg), msg,
                    "Provide substantive content of at least 20 characters.");
        }
    }
}
