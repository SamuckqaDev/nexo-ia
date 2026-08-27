package com.nexoia.workspace.tool;

import com.nexoia.provider.dto.ToolExecutionStatus;
import com.nexoia.workspace.change.model.WorkspaceChangeOperation;
import java.util.UUID;

/** Safe model-facing evidence that a server-generated change preview is waiting for approval. */
public record WorkspaceChangeProposalResult(
        ToolExecutionStatus status,
        UUID changeId,
        WorkspaceChangeOperation operation,
        String path,
        String beforeSha256,
        String afterSha256,
        Integer replacementCount,
        boolean approvalRequired,
        String message) {}
