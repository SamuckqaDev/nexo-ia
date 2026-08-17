package com.nexoia.auth.session.exception;

import com.nexoia.shared.exception.ConflictApplicationException;

public class CurrentSessionRevocationException extends ConflictApplicationException {

    public CurrentSessionRevocationException() {
        super("The current session must be ended using logout");
    }
}
