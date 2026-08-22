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
        Instant createdAt,
        Instant updatedAt) {}
