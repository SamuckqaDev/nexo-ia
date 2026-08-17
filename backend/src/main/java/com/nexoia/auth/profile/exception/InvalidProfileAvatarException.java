package com.nexoia.auth.profile.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class InvalidProfileAvatarException extends ApplicationException {
    public InvalidProfileAvatarException() {
        super(HttpStatus.BAD_REQUEST, "Use a PNG, JPEG, or WebP image up to 2 MB");
    }
}
