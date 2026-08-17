package com.nexoia.auth.user.exception;

import com.nexoia.shared.exception.ConflictApplicationException;

public class OwnerStatusChangeException extends ConflictApplicationException {

    public OwnerStatusChangeException() {
        super("The installation Owner cannot be disabled");
    }
}
