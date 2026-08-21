package com.nexoia.knowledge.graph.dto;

public record KnowledgeGraphEdgeResponse(
        String id,
        KnowledgeGraphRelation relation,
        String fromId,
        String toId,
        Double similarity) {
}
