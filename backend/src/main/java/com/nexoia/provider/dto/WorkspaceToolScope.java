package com.nexoia.provider.dto;

import com.nexoia.workspace.model.WorkspaceAccessMode;
import java.util.UUID;

/**
 * Server-resolved scope for the governed workspace read tools. It carries only internal server state —
 * never an absolute path, mount, or credential — and is never serialized into a model-facing schema.
 * The tool resolves and reads files through the centralized resolver using {@code workspaceId} and the
 * authenticated {@code userId}; the model only ever supplies workspace-relative paths.
 */
public record WorkspaceToolScope(
        UUID userId,
        UUID conversationId,
        UUID assistantMessageId,
        UUID correlationId,
        UUID workspaceId,
        String workspaceName,
        WorkspaceAccessMode accessMode,
        boolean available) {}
