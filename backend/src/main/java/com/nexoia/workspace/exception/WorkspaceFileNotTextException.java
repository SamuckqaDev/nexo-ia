package com.nexoia.workspace.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/** Raised when a workspace file cannot be represented safely as UTF-8 text. */
public class WorkspaceFileNotTextException extends ApplicationException {

    public WorkspaceFileNotTextException() {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The requested workspace file is not UTF-8 text");
    }
}
