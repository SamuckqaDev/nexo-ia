package com.nexoia.knowledge.embedding.service;

import com.nexoia.knowledge.embedding.dto.EmbeddingCommand;
import com.nexoia.knowledge.embedding.dto.EmbeddingOutcome;
import com.nexoia.provider.model.ProviderType;

/**
 * The provider boundary for embeddings. Mirrors {@code ChatCompletionClient}'s adapter shape — a real
 * protocol read directly behind a project-owned interface, not a Spring AI wrapper — because embeddings
 * use the same per-user registered endpoint as chat. See D-021 and D-026.
 */
public interface EmbeddingClient {

    boolean supports(ProviderType providerType);

    EmbeddingOutcome embed(EmbeddingCommand command);
}
