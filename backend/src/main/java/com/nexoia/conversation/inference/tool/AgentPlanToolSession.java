package com.nexoia.conversation.inference.tool;

import com.nexoia.provider.dto.ToolExecutionEvidence;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/** Request-local plan callback plus the sanitized evidence produced by its executions. */
public record AgentPlanToolSession(
        ToolCallback callback,
        List<ToolExecutionEvidence> evidence) {}
