package com.nexoia.conversation.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank @Size(max = 12000) String content,
        Boolean thinkingEnabled) {

    public SendMessageRequest {
        thinkingEnabled = Boolean.TRUE.equals(thinkingEnabled);
    }
}
