package com.nexoia.provider.dto;

import com.nexoia.provider.model.ProviderType;
import java.util.List;

/**
 * A provider-neutral inference request. The endpoint is the user's own registered provider, so it
 * must already have passed the endpoint guard before reaching a client.
 */
public record ChatCompletionCommand(
        ProviderType providerType,
        String endpoint,
        String model,
        List<ChatCompletionMessage> messages,
        boolean thinkingEnabled) {}
