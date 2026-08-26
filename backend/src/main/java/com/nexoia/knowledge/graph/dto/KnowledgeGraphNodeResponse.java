package com.nexoia.knowledge.graph.dto;

import com.nexoia.knowledge.vault.model.VaultOwnerType;
import java.util.UUID;

public record KnowledgeGraphNodeResponse(
        String id,
        KnowledgeGraphNodeKind kind,
        UUID vaultId,
        UUID ownerId,
        VaultOwnerType ownerType,
        String ownerName,
        UUID sourceId,
        Integer ordinal,
        String label,
        String detail,
        String excerpt,
        String status) {
}
