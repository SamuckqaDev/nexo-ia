package com.nexoia.conversation.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.Size;
public record SendMessageRequest(@NotBlank @Size(max = 12000) String content) {}
