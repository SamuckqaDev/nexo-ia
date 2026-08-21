package com.nexoia.knowledge.vault.dto;

import com.nexoia.knowledge.vault.model.VaultScope;
import java.time.Instant;
import java.util.UUID;

public record VaultResponse(
        UUID id,
        String name,
        String description,
        VaultScope scope,
        UUID workspaceId,
        Instant createdAt,
        Instant updatedAt) {
}
