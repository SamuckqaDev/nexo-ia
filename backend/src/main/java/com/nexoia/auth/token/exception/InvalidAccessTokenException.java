package com.nexoia.auth.token.exception;

import com.nexoia.shared.exception.UnauthorizedApplicationException;

public class InvalidAccessTokenException extends UnauthorizedApplicationException {

    public InvalidAccessTokenException() {
        super("The access token is invalid or has been revoked");
    }
}
