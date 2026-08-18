package com.nexoia.provider.dto;

import com.nexoia.provider.model.ProviderType;
import java.time.Instant;
import java.util.UUID;

public record ProviderConfigurationResponse(
        UUID id, ProviderType providerType, String displayName, String endpoint,
        String selectedModel, boolean enabled, Instant lastConnectedAt) {
}
