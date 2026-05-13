package com.backend.nmcomputercare.contactForm.controllerAdvice;

import com.backend.nmcomputercare.contactForm.exceptions.EmptyFieldException;
import com.backend.nmcomputercare.utils.ExceptionHandlerReporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ContactFormControllerAdviceTest {

    private final ContactFormControllerAdvice advice = new ContactFormControllerAdvice();

    @AfterEach
    void clearReporter() {
        ExceptionHandlerReporter.setResolveIssueDetails(null);
        ExceptionHandlerReporter.setExceptionDate(null);
    }

    @Test
    void emptyFieldReturnsStructuredBadRequest() {
        LocalDateTime occurredAt = LocalDateTime.now();
        ExceptionHandlerReporter.setResolveIssueDetails("Please provide name");
        ExceptionHandlerReporter.setExceptionDate(occurredAt);

        var response = advice.handleEmptyField(new EmptyFieldException("Name is empty"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResolveIssueResponse()).isEqualTo("Please provide name");
        assertThat(response.getBody().getErrorOccurredDate()).isEqualTo(occurredAt);
    }
}
