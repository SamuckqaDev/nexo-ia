package com.nexoia.workspace.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Raised when a file exceeds the configured read/preview byte limit, or is binary rather than text. */
public class WorkspaceFileTooLargeException extends ApplicationException {

    public WorkspaceFileTooLargeException() {
        super(HttpStatus.PAYLOAD_TOO_LARGE, "The requested file is too large or not previewable as text");
    }
}
