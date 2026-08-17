package com.nexoia.auth.session.exception;

import com.nexoia.shared.exception.UnauthorizedApplicationException;

public class InvalidCredentialsException extends UnauthorizedApplicationException {

    public InvalidCredentialsException() {
        super("Invalid username, email, or password");
    }
}
