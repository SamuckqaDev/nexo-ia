package com.nexoia.auth.profile.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ProfileAvatarNotFoundException extends ApplicationException {
    public ProfileAvatarNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Profile avatar not found");
    }
}
