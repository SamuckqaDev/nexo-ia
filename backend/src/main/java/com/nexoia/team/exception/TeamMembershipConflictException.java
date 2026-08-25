package com.nexoia.team.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Raised when a user is already a member of the Team. */
public class TeamMembershipConflictException extends ApplicationException {

    public TeamMembershipConflictException() {
        super(HttpStatus.CONFLICT, "This user is already a member of the Team");
    }
}
