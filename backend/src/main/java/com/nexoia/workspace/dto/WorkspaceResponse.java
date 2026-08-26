package com.nexoia.workspace.dto;

import com.nexoia.workspace.model.WorkspaceAccessMode;
import com.nexoia.workspace.model.WorkspaceStatus;
import com.nexoia.workspace.model.WorkspaceStorageType;
import java.time.Instant;
import java.util.UUID;

/**
 * Summary of a Workspace for the owner. {@code relativePath} is Workspace-relative and safe to show;
 * absolute paths never leave the server. {@code status} is a light live check (bound roots resolve to
 * AVAILABLE or MISSING); the status endpoint carries the full inspection.
 */
public record WorkspaceResponse(
        UUID id,
        String name,
        WorkspaceStorageType storageType,
        WorkspaceAccessMode accessMode,
        WorkspaceStatus status,
        String relativePath,
        Instant lastScannedAt,
        Instant createdAt,
        Instant updatedAt) {}
