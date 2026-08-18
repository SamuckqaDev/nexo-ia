package com.nexoia.conversation.inference.dto.event;

import com.nexoia.provider.model.TokenSource;

/**
 * Token accounting for the finished request. {@code tokenSource} states whether the counts were
 * reported by the provider or estimated by Nexo IA.
 */
public record UsageEvent(
        Integer inputTokens,
        Integer outputTokens,
        TokenSource tokenSource,
        long latencyMs) {}
