package com.nexoia.knowledge.retrieval.tool;

import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.provider.dto.ToolExecutionStatus;
import java.util.List;

/** Safe, bounded result returned to the model by {@code search_knowledge}. */
public record SearchKnowledgeResult(
        ToolExecutionStatus status,
        List<CitationResponse> citations,
        String message) {}
