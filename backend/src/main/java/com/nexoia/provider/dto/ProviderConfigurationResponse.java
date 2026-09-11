package com.nexoia.provider.dto;

import com.nexoia.provider.model.ProviderType;
import java.time.Instant;
import java.util.UUID;

public record ProviderConfigurationResponse(
        UUID id, ProviderType providerType, String displayName, String endpoint,
        String selectedModel, boolean enabled, boolean credentialConfigured, Instant lastConnectedAt) {

    public ProviderConfigurationResponse(
            UUID id,
            ProviderType providerType,
            String displayName,
            String endpoint,
            String selectedModel,
            boolean enabled,
            Instant lastConnectedAt) {
        this(id, providerType, displayName, endpoint, selectedModel, enabled, false, lastConnectedAt);
    }
}
