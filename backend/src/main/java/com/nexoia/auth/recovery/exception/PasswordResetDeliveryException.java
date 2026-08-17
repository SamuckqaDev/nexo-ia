package com.nexoia.auth.recovery.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class PasswordResetDeliveryException extends ApplicationException {

    public PasswordResetDeliveryException(Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Password reset delivery is temporarily unavailable", cause);
    }
}
