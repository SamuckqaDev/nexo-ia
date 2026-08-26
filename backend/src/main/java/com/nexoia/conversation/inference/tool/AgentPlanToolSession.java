package com.nexoia.conversation.inference.tool;

import com.nexoia.provider.dto.ToolExecutionEvidence;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.ai.tool.ToolCallback;

/** Request-local plan callback plus the sanitized evidence produced by its executions. */
public record AgentPlanToolSession(
        ToolCallback callback,
        List<ToolExecutionEvidence> evidence,
        Consumer<List<ToolExecutionEvidence>> fallbackCompletion) {

    /** Completes the deterministic visible plan only when the model never replaced it. */
    public void completeFallback(List<ToolExecutionEvidence> executionEvidence) {
        fallbackCompletion.accept(List.copyOf(executionEvidence));
    }

    public void completeFallback() {
        completeFallback(List.of());
    }
}
