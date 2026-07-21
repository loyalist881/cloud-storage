package com.github.loyalist.registrationprocess.exception;

public class RegistrationException extends RuntimeException {
    public RegistrationException(String message, Throwable cause) {
            super(message, cause);
    }
    public RegistrationException(String message) {
            super(message);
    }
}

