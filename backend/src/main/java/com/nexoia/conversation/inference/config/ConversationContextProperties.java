package com.nexoia.conversation.inference.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bounds the history handed to a model.
 *
 * <p>The character ratio is an approximation, not a tokenizer. Nexo IA uses it only to decide how
 * much history fits, and never reports an estimated count as a provider measurement.
 */
@ConfigurationProperties(prefix = "nexo.conversation.context")
public record ConversationContextProperties(int tokenBudget, int estimatedCharactersPerToken) {

    public int estimateTokens(String content) {
        return content == null || content.isEmpty()
                ? 0
                : Math.ceilDiv(content.length(), Math.max(1, estimatedCharactersPerToken));
    }
}
