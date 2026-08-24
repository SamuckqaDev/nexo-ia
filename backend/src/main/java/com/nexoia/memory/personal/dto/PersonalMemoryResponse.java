package com.nexoia.memory.personal.dto;

import java.time.Instant;
import java.util.UUID;

public record PersonalMemoryResponse(
        UUID id,
        String content,
        UUID sourceConversationId,
        Instant createdAt,
        Instant updatedAt) {}
