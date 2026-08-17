package com.nexoia.auth.session.exception;

import com.nexoia.shared.exception.UnauthorizedApplicationException;

public class UnauthenticatedSessionException extends UnauthorizedApplicationException {

    public UnauthenticatedSessionException() {
        super("An authenticated session is required");
    }
}
