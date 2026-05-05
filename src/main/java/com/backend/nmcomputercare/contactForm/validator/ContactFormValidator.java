package com.backend.nmcomputercare.contactForm.validator;

import com.backend.nmcomputercare.contactForm.dtos.ContactFormRequest;
import com.backend.nmcomputercare.contactForm.exceptions.EmptyFieldException;
import com.backend.nmcomputercare.utils.ExceptionAdvice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ContactFormValidator {

    private final ExceptionAdvice advice;

    public void validate(ContactFormRequest request) throws IllegalArgumentException {

        // Validate name
        if (isNullOrEmpty(request.getName())) {
            final var ERROR_MESSAGE = "Name is empty ";
            final  var RESOLVE_ISSUE ="Please provide name";
            throw advice.throwExceptionAndAdvice(new EmptyFieldException(ERROR_MESSAGE),ERROR_MESSAGE,RESOLVE_ISSUE);
        }

        // Validate email (simple regex check)
        if (isNullOrEmpty(request.getEmail()) || !request.getEmail().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            final var ERROR_MESSAGE = "Message is empty ";
            final  var RESOLVE_ISSUE ="Please provide message with less 300 characters";
            throw advice.throwExceptionAndAdvice(new EmptyFieldException(ERROR_MESSAGE),ERROR_MESSAGE,RESOLVE_ISSUE);
        }

        // Validate numbers (digits only)
        if (isNullOrEmpty(request.getNumbers()) || !request.getNumbers().matches("^[0-9]+$")) {
            final var ERROR_MESSAGE = "Numbers are empty ";
            final  var RESOLVE_ISSUE ="Please provide cellphone numbers";
            throw advice.throwExceptionAndAdvice(new EmptyFieldException(ERROR_MESSAGE),ERROR_MESSAGE,RESOLVE_ISSUE);        }

        // Validate service
        if (isNullOrEmpty(request.getService())) {
            final var ERROR_MESSAGE = "Service is empty ";
            final  var RESOLVE_ISSUE ="Please provide service type";
            throw advice.throwExceptionAndAdvice(new EmptyFieldException(ERROR_MESSAGE),ERROR_MESSAGE,RESOLVE_ISSUE);
        }

        // Validate message
        if (isNullOrEmpty(request.getMessage())) {
            final var ERROR_MESSAGE = "Message is empty ";
            final  var RESOLVE_ISSUE ="Please provide message with less 300 characters";
            throw advice.throwExceptionAndAdvice(new EmptyFieldException(ERROR_MESSAGE),ERROR_MESSAGE,RESOLVE_ISSUE);
        }

        // Validate message
        if (!isNullOrEmpty(request.getMessage()) && request.getMessage().length() > 300) {
            final var ERROR_MESSAGE = "Message has 300 characters more ";
            final  var RESOLVE_ISSUE ="Please provide message with less 300 characters";
            throw advice.throwExceptionAndAdvice(new EmptyFieldException(ERROR_MESSAGE),ERROR_MESSAGE,RESOLVE_ISSUE);
        }

    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }


}
