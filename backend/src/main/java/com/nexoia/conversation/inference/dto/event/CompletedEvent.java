package com.nexoia.conversation.inference.dto.event;

import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CompletedEvent(UUID messageId, String content, Instant completedAt, List<CitationResponse> citations) {}
