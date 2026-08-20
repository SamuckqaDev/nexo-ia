package com.nexoia.conversation.inference.dto.event;

import com.nexoia.provider.model.TokenSource;

/**
 * Token accounting for the finished request. {@code tokenSource} states whether input and output
 * were reported by the provider or estimated by Nexo IA. {@code contextTokensUsed} is the provider
 * prompt count; {@code contextTokenBudget} is Nexo's configured history-assembly budget, not a
 * model-specific context-window capacity.
 */
public record UsageEvent(
        Integer inputTokens,
        Integer outputTokens,
        Long totalTokens,
        Integer contextTokensUsed,
        int contextTokenBudget,
        TokenSource tokenSource,
        long latencyMs) {}
