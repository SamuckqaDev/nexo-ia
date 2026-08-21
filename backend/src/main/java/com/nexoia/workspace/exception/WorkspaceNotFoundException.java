package com.nexoia.workspace.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class WorkspaceNotFoundException extends ApplicationException {

    public WorkspaceNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Workspace not found");
    }
}
