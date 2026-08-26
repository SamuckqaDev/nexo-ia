package com.nexoia.workspace.exception;

import com.nexoia.shared.exception.ConflictApplicationException;

/** Raised when a workspace remains referenced by a Knowledge Vault or another governed resource. */
public class WorkspaceInUseException extends ConflictApplicationException {

    public WorkspaceInUseException(Throwable cause) {
        super("This workspace is still in use and cannot be deleted", cause);
    }
}
