package com.nexoia.conversation.chat.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        String title,
        UUID providerConfigurationId,
        String selectedModel,
        List<UUID> knowledgeVaultIds,
        UUID workspaceId,
        UUID workspaceBindingId,
        Instant createdAt,
        Instant updatedAt) {

    public ConversationResponse(
            UUID id,
            String title,
            UUID providerConfigurationId,
            String selectedModel,
            List<UUID> knowledgeVaultIds,
            UUID workspaceId,
            Instant createdAt,
            Instant updatedAt) {
        this(id, title, providerConfigurationId, selectedModel, knowledgeVaultIds, workspaceId, null,
                createdAt, updatedAt);
    }
}
