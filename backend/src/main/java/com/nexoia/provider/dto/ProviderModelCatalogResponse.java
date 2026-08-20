package com.nexoia.provider.dto;

import com.nexoia.provider.model.ProviderCatalogStatus;
import com.nexoia.provider.model.ProviderType;
import java.util.List;
import java.util.UUID;

public record ProviderModelCatalogResponse(
        UUID providerConfigurationId,
        ProviderType providerType,
        String displayName,
        String selectedModel,
        ProviderCatalogStatus status,
        List<ProviderModelResponse> models,
        String message) {}
