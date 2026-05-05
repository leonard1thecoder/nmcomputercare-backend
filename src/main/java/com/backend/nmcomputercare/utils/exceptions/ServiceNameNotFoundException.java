package com.backend.nmcomputercare.utils.exceptions;

public class ServiceNameNotFoundException extends RuntimeException {
    public ServiceNameNotFoundException(String message) {
        super(message);
    }
}
