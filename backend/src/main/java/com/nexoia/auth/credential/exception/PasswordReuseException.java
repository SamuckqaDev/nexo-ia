package com.nexoia.auth.credential.exception;

import com.nexoia.shared.exception.ConflictApplicationException;

public class PasswordReuseException extends ConflictApplicationException {

    public PasswordReuseException() {
        super("The new password must be different from the current password");
    }
}
