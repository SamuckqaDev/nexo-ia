package com.nexoia.provider.dto;

import com.nexoia.provider.model.TokenSource;
import java.util.List;

/**
 * The terminal result of a streamed completion. {@code cancelled} means the caller stopped reading
 * before the provider signalled completion, so the content is a partial answer.
 */
public record ChatCompletionOutcome(
        String content,
        Integer inputTokens,
        Integer outputTokens,
        TokenSource tokenSource,
        boolean cancelled,
        String doneReason,
        List<ToolExecutionEvidence> toolExecutions) {

    public ChatCompletionOutcome {
        toolExecutions = List.copyOf(toolExecutions);
    }

    public ChatCompletionOutcome(
            String content,
            Integer inputTokens,
            Integer outputTokens,
            TokenSource tokenSource,
            boolean cancelled,
            String doneReason) {
        this(content, inputTokens, outputTokens, tokenSource, cancelled, doneReason, List.of());
    }
}
