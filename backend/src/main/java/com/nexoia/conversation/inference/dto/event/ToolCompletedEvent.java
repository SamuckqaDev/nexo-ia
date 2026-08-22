package com.nexoia.conversation.inference.dto.event;

import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.provider.dto.ToolExecutionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ToolCompletedEvent(
        UUID executionId,
        String toolName,
        ToolExecutionStatus status,
        long durationMs,
        List<CitationResponse> citations,
        Instant completedAt) {}
