package com.nexoia.conversation.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateConversationRequest(
        @NotBlank @Size(max = 160) String title,
        UUID workspaceId) {

    public CreateConversationRequest(String title) {
        this(title, null);
    }
}
