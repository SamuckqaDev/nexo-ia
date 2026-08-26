package com.nexoia.workspace.tool;

import com.nexoia.provider.dto.ToolExecutionStatus;

/** Bounded Git diff returned for a single authorized workspace-relative path. */
public record WorkspaceGitDiffResult(
        ToolExecutionStatus status,
        String path,
        String diff,
        boolean truncated,
        String message) {}
