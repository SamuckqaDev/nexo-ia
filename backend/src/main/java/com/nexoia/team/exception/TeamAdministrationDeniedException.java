package com.nexoia.team.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Raised when the caller does not administer the Team they are trying to change. */
public class TeamAdministrationDeniedException extends ApplicationException {

    public TeamAdministrationDeniedException() {
        super(HttpStatus.FORBIDDEN, "You do not administer this Team");
    }
}
