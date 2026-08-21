package com.nexoia.provider.service;

import com.nexoia.provider.dto.CreateProviderRequest;
import com.nexoia.provider.dto.ProviderConfigurationResponse;
import com.nexoia.provider.exception.ProviderConfigurationNotFoundException;
import com.nexoia.provider.exception.ProviderConfigurationConflictException;
import com.nexoia.provider.model.ProviderConfiguration;
import com.nexoia.provider.repository.ProviderConfigurationRepository;
import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProviderRegistryService {
    private final ProviderConfigurationRepository repository;
    private final ProviderEndpointNormalizer endpointNormalizer;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<ProviderConfigurationResponse> list(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtAsc(userId).stream().map(this::response).toList();
    }

    @Transactional
    public ProviderConfigurationResponse create(UUID userId, CreateProviderRequest request) {
        String endpoint = normalizeEndpoint(request.endpoint());
        if (repository.existsByUserIdAndEndpoint(userId, endpoint)) {
            throw new ProviderConfigurationConflictException();
        }
        ProviderConfiguration provider = repository.save(ProviderConfiguration.builder().id(UUID.randomUUID())
                .userId(userId).providerType(request.providerType()).displayName(request.displayName().trim())
                .endpoint(endpoint).selectedModel(request.selectedModel()).enabled(true).build());
        audit.record(RecordAuditCommand.success(
                AuditAction.PROVIDER_CREATED, userId, null, AuditTargetType.PROVIDER, provider.getId()));
        return response(provider);
    }

    @Transactional
    public ProviderConfigurationResponse update(UUID userId, UUID providerId, CreateProviderRequest request) {
        ProviderConfiguration provider = repository.findByIdAndUserId(providerId, userId)
                .orElseThrow(ProviderConfigurationNotFoundException::new);
        String endpoint = normalizeEndpoint(request.endpoint());
        if (repository.existsByUserIdAndEndpointAndIdNot(userId, endpoint, providerId)) {
            throw new ProviderConfigurationConflictException();
        }
        provider.update(request.displayName().trim(), endpoint, request.selectedModel(), true);
        audit.record(RecordAuditCommand.success(
                AuditAction.PROVIDER_UPDATED, userId, null, AuditTargetType.PROVIDER, providerId));
        return response(repository.save(provider));
    }

    @Transactional
    public void remove(UUID userId, UUID providerId) {
        ProviderConfiguration provider = repository.findByIdAndUserId(providerId, userId)
                .orElseThrow(ProviderConfigurationNotFoundException::new);
        repository.delete(provider);
        audit.record(RecordAuditCommand.success(
                AuditAction.PROVIDER_REMOVED, userId, null, AuditTargetType.PROVIDER, providerId));
    }

    private String normalizeEndpoint(String endpoint) {
        return endpointNormalizer.normalize(endpoint);
    }

    private ProviderConfigurationResponse response(ProviderConfiguration provider) {
        return new ProviderConfigurationResponse(provider.getId(), provider.getProviderType(), provider.getDisplayName(),
                provider.getEndpoint(), provider.getSelectedModel(), provider.isEnabled(), provider.getLastConnectedAt());
    }
}
