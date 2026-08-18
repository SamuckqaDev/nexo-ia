package com.nexoia.provider.dto;

import java.util.List;

/**
 * The Ollama {@code /api/chat} request body. Streaming is always enabled; Nexo IA reads the response
 * incrementally so a cancellation can stop generation.
 */
public record OllamaChatRequest(String model, List<ChatCompletionMessage> messages, boolean stream) {}
