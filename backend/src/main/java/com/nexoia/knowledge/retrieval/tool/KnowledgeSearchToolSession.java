package com.nexoia.knowledge.retrieval.tool;

import com.nexoia.provider.dto.ToolExecutionEvidence;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/** Request-local callback plus the evidence produced by its bounded executions. */
public record KnowledgeSearchToolSession(
        ToolCallback callback,
        List<ToolExecutionEvidence> evidence) {}
