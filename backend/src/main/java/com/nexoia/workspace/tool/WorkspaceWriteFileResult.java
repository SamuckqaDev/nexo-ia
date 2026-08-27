package com.nexoia.workspace.tool;

import com.nexoia.provider.dto.ToolExecutionStatus;

/** Safe, bounded evidence returned after a Workspace file write. */
public record WorkspaceWriteFileResult(
        ToolExecutionStatus status,
        String path,
        boolean created,
        long sizeBytes,
        String sha256,
        String message) {}
