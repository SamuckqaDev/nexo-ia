package com.nexoia.provider.dto;

import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Bounded, persistable evidence produced by one governed tool call. */
public record ToolExecutionEvidence(
        UUID executionId,
        String toolName,
        ToolExecutionStatus status,
        long durationMs,
        List<CitationResponse> citations,
        Instant completedAt) {

    public ToolExecutionEvidence {
        citations = List.copyOf(citations);
    }
}
