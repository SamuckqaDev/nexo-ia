package com.nexoia.provider.dto;

import java.time.Instant;

public record ProviderModelResponse(
        String name,
        Instant modifiedAt,
        Long size,
        Boolean toolCallingSupported) {
}
