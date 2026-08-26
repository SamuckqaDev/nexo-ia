package com.nexoia.workspace.dto;

import com.nexoia.workspace.model.WorkspaceEntryType;
import java.time.Instant;

/** One entry inside a Workspace directory listing. {@code path} is always Workspace-relative. */
public record WorkspaceTreeEntryResponse(
        String path,
        String name,
        WorkspaceEntryType type,
        Long sizeBytes,
        Instant modifiedAt) {}
