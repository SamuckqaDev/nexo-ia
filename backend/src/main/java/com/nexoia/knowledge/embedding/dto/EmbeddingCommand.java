package com.nexoia.knowledge.embedding.dto;

import com.nexoia.provider.model.ProviderType;
import java.util.List;

/**
 * A provider-neutral embedding request. Mirrors {@code ChatCompletionCommand}'s shape: the endpoint is
 * the user's own registered provider, already past the endpoint guard before reaching a client.
 */
public record EmbeddingCommand(
        ProviderType providerType,
        String endpoint,
        String model,
        List<String> inputs) {
}
