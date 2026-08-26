package com.nexoia.workspace.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

/**
 * Raised when the Workspace structure, a target file's content, or the Git HEAD moved since the
 * fingerprint captured for a read or an approved preview. The stale operation is refused, never
 * applied to changed content.
 */
public class WorkspaceChangedException extends ApplicationException {

    public WorkspaceChangedException() {
        super(HttpStatus.CONFLICT, "The workspace changed since it was last read; refresh and retry");
    }
}
