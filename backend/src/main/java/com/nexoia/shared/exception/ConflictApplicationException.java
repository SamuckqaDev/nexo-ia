package com.nexoia.shared.exception;

import org.springframework.http.HttpStatus;

public abstract class ConflictApplicationException extends ApplicationException {

    protected ConflictApplicationException(String message) {
        super(HttpStatus.CONFLICT, message);
    }

    protected ConflictApplicationException(String message, Throwable cause) {
        super(HttpStatus.CONFLICT, message, cause);
    }
}
