package com.nexoia.workspace.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/**
 * Raised when a path resolves but must not be served — a symlink escaping the root, a protected
 * sensitive file, or a target outside the caller's authorized Workspace.
 */
public class WorkspaceAccessDeniedException extends ApplicationException {

    public WorkspaceAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "Access to the requested workspace path is not allowed");
    }
}
