package com.nexoia.workspace.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/**
 * Raised when a requested relative path is malformed or escapes the Workspace root — absolute paths,
 * {@code ..} traversal, NUL bytes, empty segments or over-long input. The public message never echoes
 * the offending path.
 */
public class WorkspaceInvalidPathException extends ApplicationException {

    public WorkspaceInvalidPathException() {
        super(HttpStatus.BAD_REQUEST, "The requested path is not a valid workspace-relative path");
    }
}
