package com.nexoia.auth.bootstrap.exception;

import com.nexoia.shared.exception.ConflictApplicationException;

public class BootstrapAlreadyCompletedException extends ConflictApplicationException {

    public BootstrapAlreadyCompletedException() {
        super("Installation bootstrap has already been completed");
    }
}
