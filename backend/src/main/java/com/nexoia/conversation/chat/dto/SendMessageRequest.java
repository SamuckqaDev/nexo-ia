package com.nexoia.conversation.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record SendMessageRequest(
        @NotBlank @Size(max = 12000) String content,
        Boolean thinkingEnabled,
        @Size(max = 8) List<UUID> knowledgeVaultIds) {

    public SendMessageRequest {
        thinkingEnabled = Boolean.TRUE.equals(thinkingEnabled);
        knowledgeVaultIds = knowledgeVaultIds == null ? List.of() : knowledgeVaultIds;
    }
}
