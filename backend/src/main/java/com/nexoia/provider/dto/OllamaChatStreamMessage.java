package com.nexoia.provider.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The message fragment of one Ollama chat stream line. Thinking and final content remain separate so
 * provider reasoning can be streamed ephemerally without entering persisted conversation history.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatStreamMessage(String role, String content, String thinking) {}
