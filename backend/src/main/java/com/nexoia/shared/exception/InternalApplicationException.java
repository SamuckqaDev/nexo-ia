package com.nexoia.shared.exception;

import org.springframework.http.HttpStatus;

public final class InternalApplicationException extends ApplicationException {

    public InternalApplicationException(Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred", cause);
    }
}
