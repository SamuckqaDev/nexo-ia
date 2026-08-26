package com.nexoia.workspace.tool;

import com.nexoia.provider.dto.ToolExecutionStatus;

/**
 * Model-facing result of {@code workspace_read_file}: a line-numbered excerpt of a workspace-relative
 * file plus its content hash, so the model can cite exact lines and detect later changes.
 */
public record WorkspaceReadFileResult(
        ToolExecutionStatus status,
        String path,
        String numberedContent,
        int startLine,
        int endLine,
        int totalLines,
        String sha256,
        boolean truncated,
        String message) {}
