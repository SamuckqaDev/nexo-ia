package com.nexoia.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateConversationModelRequest(
        @NotNull UUID providerConfigurationId,
        @NotBlank @Size(max = 160) String selectedModel) {}
