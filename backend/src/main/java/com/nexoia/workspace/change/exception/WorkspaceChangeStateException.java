package com.nexoia.workspace.change.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class WorkspaceChangeStateException extends ApplicationException {

    public WorkspaceChangeStateException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
