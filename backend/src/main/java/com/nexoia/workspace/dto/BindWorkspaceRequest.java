package com.nexoia.workspace.dto;

import com.nexoia.workspace.model.WorkspaceAccessMode;
import com.nexoia.workspace.model.WorkspaceStorageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Binds a Workspace to server-side storage. The client never sends an absolute path: {@code MANAGED}
 * ignores {@code relativePath}, and {@code MOUNTED} requires a path relative to the configured import
 * root, validated server-side.
 */
public record BindWorkspaceRequest(
        @NotNull WorkspaceStorageType storageType,
        @Size(max = 1024) String relativePath,
        @NotNull WorkspaceAccessMode accessMode) {}
