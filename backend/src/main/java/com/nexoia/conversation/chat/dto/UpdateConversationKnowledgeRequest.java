package com.nexoia.conversation.chat.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record UpdateConversationKnowledgeRequest(
        @Size(max = 8) List<UUID> vaultIds) {

    public UpdateConversationKnowledgeRequest {
        vaultIds = vaultIds == null ? List.of() : List.copyOf(vaultIds);
    }
}
