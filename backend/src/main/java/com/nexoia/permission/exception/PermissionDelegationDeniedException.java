package com.nexoia.permission.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Raised when an actor tries to grant an authority or profile beyond their own — fail-closed. */
public class PermissionDelegationDeniedException extends ApplicationException {

    public PermissionDelegationDeniedException() {
        super(HttpStatus.FORBIDDEN, "You cannot grant access beyond your own authority");
    }
}
