package com.nexoia.workspace.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class WorkspaceBindingNotFoundException extends ApplicationException {

    public WorkspaceBindingNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Workspace device binding not found");
    }
}
