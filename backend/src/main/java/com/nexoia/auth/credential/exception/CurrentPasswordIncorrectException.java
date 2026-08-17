package com.nexoia.auth.credential.exception;

import com.nexoia.shared.exception.UnauthorizedApplicationException;

public class CurrentPasswordIncorrectException extends UnauthorizedApplicationException {

    public CurrentPasswordIncorrectException() {
        super("The current password is incorrect");
    }
}
