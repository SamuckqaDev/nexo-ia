package com.nexoia.workspace.change.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class WorkspaceChangeNotFoundException extends ApplicationException {

    public WorkspaceChangeNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Workspace change was not found");
    }
}
