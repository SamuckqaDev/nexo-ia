package com.nexoia.knowledge.retrieval.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bounds retrieval independently of the chat context budget (D-026): {@code topK} and
 * {@code minimumScore} shape the vector search itself, {@code contextTokenBudget} caps the cumulative
 * excerpt size handed to {@code ConversationContextAssembler}.
 */
@ConfigurationProperties(prefix = "nexo.knowledge.retrieval")
public record KnowledgeRetrievalProperties(int topK, double minimumScore, int contextTokenBudget) {
}
