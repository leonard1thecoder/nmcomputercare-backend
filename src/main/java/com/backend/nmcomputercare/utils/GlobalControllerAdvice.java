package com.backend.nmcomputercare.utils;

import com.backend.nmcomputercare.utils.exceptions.ServiceNameNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(ServiceNameNotFoundException.class)
    public ResponseEntity<List<ErrorResponse>> manageServiceTimeoutException(){
        var list = List.of(new ErrorResponse(ExceptionHandlerReporter.getResolveIssueDetails(),ExceptionHandlerReporter.getExceptionDate()));
        return new ResponseEntity<>(list, HttpStatus.BAD_REQUEST);
    }

}
