package com.nexoia.conversation.chat.dto;

import com.nexoia.conversation.chat.model.ConversationRole;
import java.time.Instant;
import java.util.UUID;

public record ConversationMessageResponse(
        UUID id,
        ConversationRole role,
        String content,
        Instant createdAt) {}
