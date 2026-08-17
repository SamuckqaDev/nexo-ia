package com.nexoia.auth.loginattempt.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class LoginThrottledException extends ApplicationException {

    public LoginThrottledException() {
        super(HttpStatus.TOO_MANY_REQUESTS,
                "Too many login attempts. Wait before trying again");
    }
}
