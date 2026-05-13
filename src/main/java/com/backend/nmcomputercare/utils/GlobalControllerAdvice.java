package com.backend.nmcomputercare.utils;

import com.backend.nmcomputercare.utils.exceptions.ServiceNameNotFoundException;
import com.backend.nmcomputercare.utils.exceptions.ServiceTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.backend.nmcomputercare.utils.exceptions.IncorrectRequestSentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(GlobalControllerAdvice.class);

    @ExceptionHandler(IncorrectRequestSentException.class)
    public ResponseEntity<ErrorResponse> handleIncorrectRequest(IncorrectRequestSentException ex) {

        // Pull the contextual details set at the throw site.
        String  details   = ExceptionHandlerReporter.getResolveIssueDetails();
        var     errorDate = ExceptionHandlerReporter.getExceptionDate();

        logger.warn("IncorrectRequestSentException | message={} details={}", ex.getMessage(), details);

        ErrorResponse body = ErrorResponse.builder()
                .resolveIssueResponse(details != null ? details : ex.getMessage())
                .errorOccurredDate(errorDate)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
	
	  @ExceptionHandler(ServiceNameNotFoundException.class)
    public ResponseEntity<List<ErrorResponse>> manageServiceTimeoutException(){
        var list = List.of(new ErrorResponse(ExceptionHandlerReporter.getResolveIssueDetails(),ExceptionHandlerReporter.getExceptionDate()));
        return new ResponseEntity<>(list, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ServiceTimeoutException.class)
    public ResponseEntity<List<ErrorResponse>> handleServiceTimeoutException(ServiceTimeoutException ex) {
        var list = List.of(ErrorResponse.builder()
                .resolveIssueResponse(ExceptionHandlerReporter.getResolveIssueDetails() != null
                        ? ExceptionHandlerReporter.getResolveIssueDetails()
                        : ex.getMessage())
                .errorOccurredDate(ExceptionHandlerReporter.getExceptionDate() != null
                        ? ExceptionHandlerReporter.getExceptionDate()
                        : LocalDateTime.now())
                .build());
        return new ResponseEntity<>(list, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
