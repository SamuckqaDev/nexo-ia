package com.nexoia.knowledge.vault.dto;

import com.nexoia.knowledge.vault.model.VaultScope;
import com.nexoia.knowledge.vault.model.VaultOwnerType;
import java.time.Instant;
import java.util.UUID;

public record VaultResponse(
        UUID id,
        String name,
        String description,
        VaultScope scope,
        UUID workspaceId,
        UUID ownerId,
        VaultOwnerType ownerType,
        String ownerName,
        boolean manageable,
        boolean writable,
        Instant createdAt,
        Instant updatedAt) {
}
