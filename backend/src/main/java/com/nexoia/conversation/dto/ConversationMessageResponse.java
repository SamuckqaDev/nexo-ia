package com.nexoia.conversation.dto;
import com.nexoia.conversation.model.ConversationRole; import java.time.Instant; import java.util.UUID;
public record ConversationMessageResponse(UUID id, ConversationRole role, String content, Instant createdAt) {}
