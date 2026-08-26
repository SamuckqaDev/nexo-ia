package com.nexoia.workspace.tool;

import com.nexoia.provider.dto.ToolExecutionStatus;
import java.util.List;

/** Read-only Git state containing no server-absolute paths. */
public record WorkspaceGitStatusResult(
        ToolExecutionStatus status,
        String branch,
        String head,
        List<String> changedPaths,
        boolean truncated,
        String message) {}
