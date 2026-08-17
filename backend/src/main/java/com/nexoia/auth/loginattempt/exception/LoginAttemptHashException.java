package com.nexoia.auth.loginattempt.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class LoginAttemptHashException extends ApplicationException {

    public LoginAttemptHashException(Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR,
                "Login protection could not process the request", cause);
    }
}
