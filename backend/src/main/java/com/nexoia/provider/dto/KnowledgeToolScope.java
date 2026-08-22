package com.nexoia.provider.dto;

import java.util.List;
import java.util.UUID;

/** Server-resolved scope for the read-only knowledge tool; never serialized into its model schema. */
public record KnowledgeToolScope(
        UUID userId,
        UUID assistantMessageId,
        UUID correlationId,
        List<UUID> authorizedVaultIds) {

    public KnowledgeToolScope {
        authorizedVaultIds = List.copyOf(authorizedVaultIds);
    }

    public boolean available() {
        return !authorizedVaultIds.isEmpty();
    }
}
