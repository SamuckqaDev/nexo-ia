package com.nexoia.conversation.chat.dto;

import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.provider.dto.ToolExecutionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ToolExecutionResponse(
        UUID id,
        String toolName,
        ToolExecutionStatus status,
        Long durationMs,
        List<CitationResponse> citations,
        Instant startedAt,
        Instant completedAt) {}
