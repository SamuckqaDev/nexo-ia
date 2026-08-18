package com.nexoia.conversation.dto;
import java.time.Instant; import java.util.UUID;
public record ConversationResponse(UUID id, String title, UUID providerConfigurationId, String selectedModel, Instant createdAt, Instant updatedAt) {}
