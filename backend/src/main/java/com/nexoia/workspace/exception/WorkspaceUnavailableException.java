package com.nexoia.workspace.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Raised when a bound Workspace's storage root or path cannot be resolved on the server. */
public class WorkspaceUnavailableException extends ApplicationException {

    public WorkspaceUnavailableException() {
        super(HttpStatus.CONFLICT, "This workspace storage is currently unavailable");
    }

    public WorkspaceUnavailableException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
