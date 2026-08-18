package com.nexoia.provider.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The message fragment of one Ollama chat stream line. Reasoning models may also emit a separate
 * thinking field, which Nexo IA intentionally ignores until reasoning display has a contract.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatStreamMessage(String role, String content) {}
