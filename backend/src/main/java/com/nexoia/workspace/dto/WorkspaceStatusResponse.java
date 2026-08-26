package com.nexoia.workspace.dto;

import com.nexoia.workspace.model.WorkspaceAccessMode;
import com.nexoia.workspace.model.WorkspaceStatus;
import com.nexoia.workspace.model.WorkspaceStorageType;
import java.time.Instant;
import java.util.List;

/**
 * The live availability of a Workspace plus safe inspection metadata. {@code reason} explains an
 * unavailable state in user-facing terms and never contains an absolute path.
 */
public record WorkspaceStatusResponse(
        WorkspaceStatus status,
        WorkspaceStorageType storageType,
        WorkspaceAccessMode accessMode,
        String relativePath,
        String structureFingerprint,
        Instant lastScannedAt,
        WorkspaceGitSummary git,
        List<String> detectedStack,
        String reason) {}
