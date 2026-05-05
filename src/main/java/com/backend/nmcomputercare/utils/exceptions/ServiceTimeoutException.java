package com.backend.nmcomputercare.utils.exceptions;

public class ServiceTimeoutException extends RuntimeException{
    public ServiceTimeoutException(String message) {
        super(message);
    }

}
