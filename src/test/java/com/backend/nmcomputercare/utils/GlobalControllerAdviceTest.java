package com.backend.nmcomputercare.utils;

import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import com.backend.nmcomputercare.utils.exceptions.ServiceNameNotFoundException;
import com.backend.nmcomputercare.utils.exceptions.ServiceTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalControllerAdviceTest {

    private final GlobalControllerAdvice advice = new GlobalControllerAdvice();

    @AfterEach
    void clearReporter() {
        ExceptionHandlerReporter.setResolveIssueDetails(null);
        ExceptionHandlerReporter.setExceptionDate(null);
    }

    @Test
    void incorrectRequestUsesReporterDetails() {
        LocalDateTime occurredAt = LocalDateTime.now();
        ExceptionHandlerReporter.setResolveIssueDetails("Fix the submitted request.");
        ExceptionHandlerReporter.setExceptionDate(occurredAt);

        var response = advice.handleIncorrectRequest(new IncorrectRequestSentException("Bad request"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResolveIssueResponse()).isEqualTo("Fix the submitted request.");
        assertThat(response.getBody().getErrorOccurredDate()).isEqualTo(occurredAt);
    }

    @Test
    void serviceNameNotFoundUsesReporterDetails() {
        LocalDateTime occurredAt = LocalDateTime.now();
        ExceptionHandlerReporter.setResolveIssueDetails("Use a supported service name.");
        ExceptionHandlerReporter.setExceptionDate(occurredAt);

        var response = advice.manageServiceTimeoutException();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).singleElement().satisfies(error -> {
            assertThat(error.getResolveIssueResponse()).isEqualTo("Use a supported service name.");
            assertThat(error.getErrorOccurredDate()).isEqualTo(occurredAt);
        });
    }

    @Test
    void serviceTimeoutReturnsUnavailable() {
        LocalDateTime occurredAt = LocalDateTime.now();
        ExceptionHandlerReporter.setResolveIssueDetails("Try again shortly.");
        ExceptionHandlerReporter.setExceptionDate(occurredAt);

        var response = advice.handleServiceTimeoutException(new ServiceTimeoutException("Timed out"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).singleElement().satisfies(error -> {
            assertThat(error.getResolveIssueResponse()).isEqualTo("Try again shortly.");
            assertThat(error.getErrorOccurredDate()).isEqualTo(occurredAt);
        });
    }
}
