package com.nexoia.conversation.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.Size;
public record CreateConversationRequest(@NotBlank @Size(max = 160) String title) {}
