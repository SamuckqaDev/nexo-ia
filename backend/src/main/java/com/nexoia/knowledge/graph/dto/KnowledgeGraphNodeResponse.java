package com.nexoia.knowledge.graph.dto;

import java.util.UUID;

public record KnowledgeGraphNodeResponse(
        String id,
        KnowledgeGraphNodeKind kind,
        UUID vaultId,
        UUID sourceId,
        Integer ordinal,
        String label,
        String detail,
        String excerpt,
        String status) {
}
