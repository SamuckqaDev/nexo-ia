package com.nexoia.provider.service;

import com.nexoia.provider.dto.ProviderConnectionTestResponse;
import com.nexoia.provider.dto.ProviderModelCatalogResponse;
import com.nexoia.provider.dto.ProviderModelResponse;
import com.nexoia.provider.exception.ProviderConfigurationNotFoundException;
import com.nexoia.provider.exception.ProviderUnavailableException;
import com.nexoia.provider.model.ProcessingLocation;
import com.nexoia.provider.model.ProviderCatalogStatus;
import com.nexoia.provider.model.ProviderConfiguration;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.repository.ProviderConfigurationRepository;
import com.nexoia.provider.secret.dto.ProviderAuthentication;
import com.nexoia.provider.secret.service.ProviderSecretService;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProviderModelCatalogService {

    private final ProviderConfigurationRepository repository;
    private final ProviderEndpointGuard endpointGuard;
    private final ProviderEndpointNormalizer endpointNormalizer;
    private final OllamaProviderService ollamaProviderService;
    private final RemoteProviderModelService remoteProviderModelService;
    private final ProviderSecretService secrets;

    @Autowired
    public ProviderModelCatalogService(
            ProviderConfigurationRepository repository,
            ProviderEndpointGuard endpointGuard,
            ProviderEndpointNormalizer endpointNormalizer,
            OllamaProviderService ollamaProviderService,
            RemoteProviderModelService remoteProviderModelService,
            ProviderSecretService secrets) {
        this.repository = repository;
        this.endpointGuard = endpointGuard;
        this.endpointNormalizer = endpointNormalizer;
        this.ollamaProviderService = ollamaProviderService;
        this.remoteProviderModelService = remoteProviderModelService;
        this.secrets = secrets;
    }

    /**
     * Resolves ownership before inspecting an endpoint. This method deliberately has no surrounding
     * transaction so the provider network request never keeps a database transaction open.
     */
    public ProviderModelCatalogResponse discover(UUID userId, UUID providerId) {
        ProviderConfiguration provider = repository.findByIdAndUserId(providerId, userId)
                .orElseThrow(ProviderConfigurationNotFoundException::new);

        if (!provider.isEnabled()) {
            return response(provider, ProviderCatalogStatus.UNAVAILABLE, List.of(),
                    "This provider configuration is disabled");
        }

        endpointGuard.verify(provider.getProviderType(), provider.getEndpoint());

        try {
            List<ProviderModelResponse> models = provider.getProviderType() == ProviderType.OLLAMA
                    ? ollamaProviderService.models(provider.getEndpoint())
                    : remoteProviderModelService.models(
                            provider.getProviderType(), provider.getEndpoint(), secrets.resolve(provider));
            if (models.isEmpty()) {
                return response(provider, ProviderCatalogStatus.EMPTY, models,
                        "No installed models were reported by this provider");
            }
            return response(provider, ProviderCatalogStatus.AVAILABLE, models, null);
        } catch (ProviderUnavailableException exception) {
            return response(provider, ProviderCatalogStatus.UNAVAILABLE, List.of(),
                    "The configured provider is currently unavailable");
        }
    }

    /**
     * Tests connectivity for an endpoint the user has not saved yet. No provider configuration is
     * read or persisted here, so there is no ownership to resolve and nothing to leak into storage.
     */
    public ProviderConnectionTestResponse testConnection(
            ProviderType providerType,
            String rawEndpoint,
            String apiKey) {
        String endpoint = endpointNormalizer.normalize(rawEndpoint);
        ProcessingLocation processingLocation = endpointGuard.verify(providerType, endpoint);

        try {
            secrets.requireFor(providerType, apiKey, false);
            ProviderAuthentication authentication = apiKey == null || apiKey.isBlank()
                    ? ProviderAuthentication.none()
                    : ProviderAuthentication.apiKey(apiKey.trim());
            List<ProviderModelResponse> models = providerType == ProviderType.OLLAMA
                    ? ollamaProviderService.models(endpoint)
                    : remoteProviderModelService.models(providerType, endpoint, authentication);
            if (models.isEmpty()) {
                return new ProviderConnectionTestResponse(providerType, endpoint, ProviderCatalogStatus.EMPTY,
                        processingLocation, models, "No installed models were reported by this provider");
            }
            return new ProviderConnectionTestResponse(providerType, endpoint, ProviderCatalogStatus.AVAILABLE,
                    processingLocation, models, null);
        } catch (ProviderUnavailableException exception) {
            return new ProviderConnectionTestResponse(providerType, endpoint, ProviderCatalogStatus.UNAVAILABLE,
                    processingLocation, List.of(), "The configured provider is currently unavailable");
        }
    }

    public ProviderConnectionTestResponse testConnection(ProviderType providerType, String rawEndpoint) {
        return testConnection(providerType, rawEndpoint, null);
    }

    private ProviderModelCatalogResponse response(
            ProviderConfiguration provider,
            ProviderCatalogStatus status,
            List<ProviderModelResponse> models,
            String message) {
        return new ProviderModelCatalogResponse(
                provider.getId(),
                provider.getProviderType(),
                provider.getDisplayName(),
                provider.getSelectedModel(),
                status,
                models,
                message);
    }
}
