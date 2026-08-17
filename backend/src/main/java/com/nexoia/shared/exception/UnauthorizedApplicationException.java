package com.nexoia.shared.exception;

import org.springframework.http.HttpStatus;

public abstract class UnauthorizedApplicationException extends ApplicationException {

    protected UnauthorizedApplicationException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    protected UnauthorizedApplicationException(String message, Throwable cause) {
        super(HttpStatus.UNAUTHORIZED, message, cause);
    }
}
