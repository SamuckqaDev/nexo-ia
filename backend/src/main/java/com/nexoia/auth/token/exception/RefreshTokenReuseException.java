package com.nexoia.auth.token.exception;

import com.nexoia.shared.exception.UnauthorizedApplicationException;

public class RefreshTokenReuseException extends UnauthorizedApplicationException {

    public RefreshTokenReuseException() {
        super("Refresh token reuse was detected and the session was revoked");
    }
}
