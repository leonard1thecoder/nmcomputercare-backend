package com.backend.nmcomputercare.subscribe.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerificationExceptionAdviceTest {

    private final VerificationExceptionAdvice advice = new VerificationExceptionAdvice();
    private final WebRequest request = mock(WebRequest.class);

    @Test
    void tokenNotFoundReturnsStructured404() {
        when(request.getDescription(false)).thenReturn("uri=/api/subscriptions/verify");

        var response = advice.handleTokenNotFound(new TokenNotFoundException("missing"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertBody(response.getBody(), 404, "TOKEN_NOT_FOUND", "/api/subscriptions/verify");
    }

    @Test
    void tokenAlreadyUsedReturnsStructured409() {
        when(request.getDescription(false)).thenReturn("uri=/api/subscriptions/verify");

        var response = advice.handleTokenAlreadyUsed(new TokenAlreadyUsedException("used"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertBody(response.getBody(), 409, "TOKEN_ALREADY_USED", "/api/subscriptions/verify");
    }

    @Test
    void tokenExpiredReturnsStructured410() {
        when(request.getDescription(false)).thenReturn("uri=/api/subscriptions/verify");

        var response = advice.handleTokenExpired(new TokenExpiredException("expired"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertBody(response.getBody(), 410, "TOKEN_EXPIRED", "/api/subscriptions/verify");
    }

    private static void assertBody(Map<String, Object> body,
                                   int status,
                                   String code,
                                   String path) {
        assertThat(body).isNotNull();
        assertThat(body).containsEntry("status", status);
        assertThat(body).containsEntry("code", code);
        assertThat(body).containsEntry("path", path);
        assertThat(body).containsKeys("timestamp", "error", "message");
    }
}
