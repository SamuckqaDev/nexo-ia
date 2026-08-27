package com.nexoia.provider.dto;

import com.nexoia.workspace.model.WorkspaceAccessMode;
import java.util.UUID;

/**
 * Server-resolved scope for governed Workspace tools. It carries only internal server state —
 * never an absolute path, mount, or credential — and is never serialized into a model-facing schema.
 * Each tool resolves files through the centralized resolver using {@code workspaceId} and the
 * authenticated {@code userId}; the model only ever supplies Workspace-relative paths. Write
 * authorization is fresh for the current explicit request and is not persisted in this scope.
 */
public record WorkspaceToolScope(
        UUID userId,
        UUID conversationId,
        UUID assistantMessageId,
        UUID correlationId,
        UUID workspaceId,
        String workspaceName,
        WorkspaceAccessMode accessMode,
        boolean available,
        UUID workspaceBindingId,
        UUID deviceId,
        String localBindingId,
        boolean writeAuthorized) {

    public WorkspaceToolScope(
            UUID userId,
            UUID conversationId,
            UUID assistantMessageId,
            UUID correlationId,
            UUID workspaceId,
            String workspaceName,
            WorkspaceAccessMode accessMode,
            boolean available,
            UUID workspaceBindingId,
            UUID deviceId,
            String localBindingId) {
        this(userId, conversationId, assistantMessageId, correlationId, workspaceId, workspaceName,
                accessMode, available, workspaceBindingId, deviceId, localBindingId, false);
    }

    public WorkspaceToolScope(
            UUID userId,
            UUID conversationId,
            UUID assistantMessageId,
            UUID correlationId,
            UUID workspaceId,
            String workspaceName,
            WorkspaceAccessMode accessMode,
            boolean available) {
        this(userId, conversationId, assistantMessageId, correlationId, workspaceId, workspaceName,
                accessMode, available, null, null, null, false);
    }

    public boolean localDevice() {
        return workspaceBindingId != null && deviceId != null && localBindingId != null;
    }
}
