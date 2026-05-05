package com.backend.nmcomputercare.utils.exceptions;

public class IncorrectRequestSentException extends RuntimeException {
    public IncorrectRequestSentException(String message) {
        super(message);
    }
}
