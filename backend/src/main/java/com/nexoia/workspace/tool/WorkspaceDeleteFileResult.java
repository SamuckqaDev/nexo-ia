package com.nexoia.workspace.tool;

import com.nexoia.provider.dto.ToolExecutionStatus;

/** Runtime evidence for one explicit file deletion. */
public record WorkspaceDeleteFileResult(
        ToolExecutionStatus status,
        String path,
        String previousSha256,
        String message) {}
