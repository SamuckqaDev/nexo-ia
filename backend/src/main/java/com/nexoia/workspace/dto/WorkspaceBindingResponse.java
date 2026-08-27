package com.nexoia.workspace.dto;

import com.nexoia.device.model.DeviceStatus;
import com.nexoia.workspace.model.WorkspaceBindingStatus;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceBindingResponse(
        UUID id,
        UUID workspaceId,
        UUID deviceId,
        String deviceName,
        DeviceStatus deviceStatus,
        String displayName,
        WorkspaceBindingStatus status,
        String structureFingerprint,
        String gitHead,
        String gitBranch,
        Instant lastSeenAt,
        Instant createdAt,
        Instant updatedAt) {}
