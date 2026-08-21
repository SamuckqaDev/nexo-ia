package com.nexoia.provider.dto;

import com.nexoia.provider.model.ProcessingLocation;
import com.nexoia.provider.model.ProviderCatalogStatus;
import com.nexoia.provider.model.ProviderType;
import java.util.List;

public record ProviderConnectionTestResponse(
        ProviderType providerType,
        String endpoint,
        ProviderCatalogStatus status,
        ProcessingLocation processingLocation,
        List<ProviderModelResponse> models,
        String message) {}
