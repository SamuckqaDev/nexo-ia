package com.nexoia.provider.dto;

import java.util.UUID;

/** Server-created ownership and provenance for the request-scoped remember tool. */
public record MemoryToolScope(
        UUID userId,
        UUID conversationId,
        UUID assistantMessageId,
        UUID correlationId) {}
