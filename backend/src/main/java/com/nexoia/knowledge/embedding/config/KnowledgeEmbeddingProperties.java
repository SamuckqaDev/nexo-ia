package com.nexoia.knowledge.embedding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The default embedding model is never required for ordinary chat and is configurable only for the
 * model identifier — vector dimensions are fixed by migration, not runtime-configurable. See D-026.
 */
@ConfigurationProperties(prefix = "nexo.knowledge.embedding")
public record KnowledgeEmbeddingProperties(String modelIdentifier) {
}
