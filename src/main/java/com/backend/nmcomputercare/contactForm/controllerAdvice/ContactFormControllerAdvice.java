package com.backend.nmcomputercare.contactForm.controllerAdvice;

import com.backend.nmcomputercare.contactForm.exceptions.EmptyFieldException;
import com.backend.nmcomputercare.utils.ErrorResponse;
import com.backend.nmcomputercare.utils.ExceptionHandlerReporter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ContactFormControllerAdvice {

    @ExceptionHandler(EmptyFieldException.class)
    public ResponseEntity<ErrorResponse> handleEmptyField(EmptyFieldException ex) {
        String details = ExceptionHandlerReporter.getResolveIssueDetails();
        LocalDateTime errorDate = ExceptionHandlerReporter.getExceptionDate();

        ErrorResponse body = ErrorResponse.builder()
                .resolveIssueResponse(details != null ? details : ex.getMessage())
                .errorOccurredDate(errorDate != null ? errorDate : LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
