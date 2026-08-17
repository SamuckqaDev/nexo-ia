package com.nexoia.auth.user.exception;

import com.nexoia.shared.exception.ConflictApplicationException;

public class UserIdentityAlreadyExistsException extends ConflictApplicationException {

    public UserIdentityAlreadyExistsException() {
        super("Username or email is already in use");
    }
}
