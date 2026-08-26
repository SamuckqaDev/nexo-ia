package com.nexoia.workspace.tool;

import com.nexoia.provider.dto.ToolExecutionStatus;
import com.nexoia.workspace.dto.WorkspaceGitSummary;
import java.util.List;

/** Model-facing result of {@code workspace_inspect_project}: detected stack labels and Git summary. */
public record WorkspaceInspectProjectResult(
        ToolExecutionStatus status,
        String workspaceName,
        List<String> detectedStack,
        WorkspaceGitSummary git,
        String message) {}
