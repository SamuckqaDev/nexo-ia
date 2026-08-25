package com.nexoia.team.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Raised when a Team does not exist, or is not visible to the caller. */
public class TeamNotFoundException extends ApplicationException {

    public TeamNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Team not found");
    }
}
