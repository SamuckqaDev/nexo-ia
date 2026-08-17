package com.nexoia.auth.token.exception;

import com.nexoia.shared.exception.UnauthorizedApplicationException;

public class InvalidRefreshTokenException extends UnauthorizedApplicationException {

    public InvalidRefreshTokenException() {
        super("The refresh token is invalid or expired");
    }
}
