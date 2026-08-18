package com.nexoia.provider.dto;

import java.util.List;

public record ProviderStatusResponse(
        String id,
        String name,
        String kind,
        String endpoint,
        boolean connected,
        List<ProviderModelResponse> models) {
}
