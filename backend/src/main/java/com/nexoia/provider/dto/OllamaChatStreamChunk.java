package com.nexoia.provider.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One newline-delimited JSON line of an Ollama {@code /api/chat} stream. Token accounting is only
 * present on the final line, where {@code done} is true.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatStreamChunk(
        OllamaChatStreamMessage message,
        boolean done,
        @JsonProperty("done_reason") String doneReason,
        @JsonProperty("prompt_eval_count") Integer promptEvalCount,
        @JsonProperty("eval_count") Integer evalCount) {

    public String contentDelta() {
        return message == null || message.content() == null ? "" : message.content();
    }
}
