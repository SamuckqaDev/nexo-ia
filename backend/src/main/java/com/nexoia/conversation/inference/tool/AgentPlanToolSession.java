package com.nexoia.conversation.inference.tool;

import com.nexoia.provider.dto.ToolExecutionEvidence;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/** Request-local plan callback plus the sanitized evidence produced by its executions. */
public record AgentPlanToolSession(
        ToolCallback callback,
        List<ToolExecutionEvidence> evidence,
        Runnable fallbackCompletion) {

    /** Completes the deterministic visible plan only when the model never replaced it. */
    public void completeFallback() {
        fallbackCompletion.run();
    }
}
