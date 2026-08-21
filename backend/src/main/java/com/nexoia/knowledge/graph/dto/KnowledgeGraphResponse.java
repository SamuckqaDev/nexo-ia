package com.nexoia.knowledge.graph.dto;

import java.util.List;

public record KnowledgeGraphResponse(
        List<KnowledgeGraphNodeResponse> nodes,
        List<KnowledgeGraphEdgeResponse> edges,
        int vaultCount,
        int sourceCount,
        int chunkCount,
        boolean truncated) {
}
