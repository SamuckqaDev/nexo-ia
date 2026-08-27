package com.nexoia.workspace.change.dto;

import com.nexoia.workspace.change.model.WorkspaceChangeOperation;
import com.nexoia.workspace.change.model.WorkspaceChangeStatus;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceChangeResponse(
        UUID id,
        UUID workspaceId,
        WorkspaceChangeOperation operation,
        WorkspaceChangeStatus status,
        String path,
        String beforeSha256,
        String afterSha256,
        Integer replacementCount,
        String beforeContent,
        String afterContent,
        boolean previewTruncated,
        String failureCode,
        Instant createdAt,
        Instant appliedAt,
        Instant revertedAt) {}
