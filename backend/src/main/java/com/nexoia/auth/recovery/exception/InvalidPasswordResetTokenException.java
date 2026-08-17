package com.nexoia.auth.recovery.exception;

import com.nexoia.shared.exception.UnauthorizedApplicationException;

public class InvalidPasswordResetTokenException extends UnauthorizedApplicationException {

    public InvalidPasswordResetTokenException() {
        super("The password reset link is invalid or expired");
    }
}
