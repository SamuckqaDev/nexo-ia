package com.nexoia.provider.dto;

import java.time.Instant;

public record OllamaModelResponse(String name, Instant modifiedAt, Long size) {
}
