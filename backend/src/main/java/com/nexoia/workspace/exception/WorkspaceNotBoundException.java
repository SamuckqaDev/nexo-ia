package com.nexoia.workspace.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Raised when an operation needs server-side files but the Workspace has no binding yet. */
public class WorkspaceNotBoundException extends ApplicationException {

    public WorkspaceNotBoundException() {
        super(HttpStatus.CONFLICT, "This workspace is not bound to server storage yet");
    }
}
