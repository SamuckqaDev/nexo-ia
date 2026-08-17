package com.nexoia.auth.session.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class SessionNotFoundException extends ApplicationException {

    public SessionNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Active session not found");
    }
}
