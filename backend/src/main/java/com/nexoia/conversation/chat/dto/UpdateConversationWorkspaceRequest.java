package com.nexoia.conversation.chat.dto;

import java.util.UUID;

/**
 * Selects the server-side Workspace bound to a conversation. A {@code null} {@code workspaceId} clears
 * the selection. The workspace must be owned by the caller; the server never trusts a workspace id
 * sent inside a chat message.
 */
public record UpdateConversationWorkspaceRequest(UUID workspaceId, UUID workspaceBindingId) {

    public UpdateConversationWorkspaceRequest(UUID workspaceId) {
        this(workspaceId, null);
    }
}
