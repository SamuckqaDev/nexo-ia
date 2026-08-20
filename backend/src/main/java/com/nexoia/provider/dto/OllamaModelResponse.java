package com.nexoia.provider.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record OllamaModelResponse(
        String name,
        @JsonProperty("modified_at") Instant modifiedAt,
        Long size) {
}
