package com.nexoia.auth.token.exception;

public class InvalidTokenConfigurationException extends IllegalStateException {

    public InvalidTokenConfigurationException(String message) {
        super(message);
    }

    public InvalidTokenConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
