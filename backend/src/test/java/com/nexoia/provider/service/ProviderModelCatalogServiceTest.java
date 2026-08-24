package com.nexoia.provider.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.provider.dto.ProviderModelResponse;
import com.nexoia.provider.exception.ProviderConfigurationNotFoundException;
import com.nexoia.provider.exception.ProviderUnavailableException;
import com.nexoia.provider.model.ProcessingLocation;
import com.nexoia.provider.model.ProviderCatalogStatus;
import com.nexoia.provider.model.ProviderConfiguration;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.repository.ProviderConfigurationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProviderModelCatalogServiceTest {

    @Mock
    private ProviderConfigurationRepository repository;
    @Mock
    private ProviderEndpointGuard endpointGuard;
    @Mock
    private OllamaProviderService ollamaProviderService;

    private ProviderModelCatalogService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ProviderModelCatalogService(repository, endpointGuard, new ProviderEndpointNormalizer(""), ollamaProviderService);
    }

    @Test
    void discoversModelsOnlyAfterResolvingProviderOwnership() {
        ProviderConfiguration provider = provider(ProviderType.OLLAMA);
        when(repository.findByIdAndUserId(providerId, userId)).thenReturn(Optional.of(provider));
        when(endpointGuard.verify(ProviderType.OLLAMA, provider.getEndpoint()))
                .thenReturn(ProcessingLocation.LOCAL);
        when(ollamaProviderService.models(provider.getEndpoint()))
                .thenReturn(List.of(new ProviderModelResponse("qwen3:8b", null, 42L, true, true)));

        var response = service.discover(userId, providerId);

        assertThat(response.status()).isEqualTo(ProviderCatalogStatus.AVAILABLE);
        assertThat(response.models()).extracting(ProviderModelResponse::name)
                .containsExactly("qwen3:8b");
        verify(endpointGuard).verify(ProviderType.OLLAMA, provider.getEndpoint());
    }

    @Test
    void neverDereferencesAProviderOwnedByAnotherUser() {
        when(repository.findByIdAndUserId(providerId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.discover(userId, providerId))
                .isInstanceOf(ProviderConfigurationNotFoundException.class);

        verify(endpointGuard, never()).verify(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
        verify(ollamaProviderService, never()).models(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void reportsUnsupportedProtocolsWithoutOpeningANetworkConnection() {
        ProviderConfiguration provider = provider(ProviderType.ANTHROPIC);
        when(repository.findByIdAndUserId(providerId, userId)).thenReturn(Optional.of(provider));

        var response = service.discover(userId, providerId);

        assertThat(response.status()).isEqualTo(ProviderCatalogStatus.UNSUPPORTED);
        assertThat(response.models()).isEmpty();
        verify(endpointGuard, never()).verify(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void turnsProviderOutagesIntoASafeTypedCatalogState() {
        ProviderConfiguration provider = provider(ProviderType.OLLAMA);
        when(repository.findByIdAndUserId(providerId, userId)).thenReturn(Optional.of(provider));
        when(ollamaProviderService.models(provider.getEndpoint()))
                .thenThrow(new ProviderUnavailableException());

        var response = service.discover(userId, providerId);

        assertThat(response.status()).isEqualTo(ProviderCatalogStatus.UNAVAILABLE);
        assertThat(response.message()).doesNotContain(provider.getEndpoint());
    }

    @Test
    void testsAnUnsavedOllamaEndpointWithoutTouchingTheRepository() {
        when(endpointGuard.verify(ProviderType.OLLAMA, "http://127.0.0.1:11434"))
                .thenReturn(ProcessingLocation.LOCAL);
        when(ollamaProviderService.models("http://127.0.0.1:11434"))
                .thenReturn(List.of(new ProviderModelResponse("qwen3:8b", null, 42L, true, true)));

        var response = service.testConnection(ProviderType.OLLAMA, "http://127.0.0.1:11434");

        assertThat(response.status()).isEqualTo(ProviderCatalogStatus.AVAILABLE);
        assertThat(response.processingLocation()).isEqualTo(ProcessingLocation.LOCAL);
        assertThat(response.models()).extracting(ProviderModelResponse::name).containsExactly("qwen3:8b");
        verify(repository, never()).findByIdAndUserId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reportsUnsupportedProviderTypesWithoutOpeningANetworkConnectionWhenTesting() {
        var response = service.testConnection(ProviderType.ANTHROPIC, "https://api.anthropic.com");

        assertThat(response.status()).isEqualTo(ProviderCatalogStatus.UNSUPPORTED);
        assertThat(response.models()).isEmpty();
        verify(endpointGuard, never()).verify(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsAMalformedEndpointBeforeTestingIt() {
        assertThatThrownBy(() -> service.testConnection(ProviderType.OLLAMA, "not-a-url"))
                .isInstanceOf(com.nexoia.provider.exception.InvalidProviderEndpointException.class);

        verify(endpointGuard, never()).verify(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void turnsAnUnreachableTestEndpointIntoASafeTypedStatus() {
        when(endpointGuard.verify(ProviderType.OLLAMA, "http://127.0.0.1:11434"))
                .thenReturn(ProcessingLocation.LOCAL);
        when(ollamaProviderService.models("http://127.0.0.1:11434"))
                .thenThrow(new ProviderUnavailableException());

        var response = service.testConnection(ProviderType.OLLAMA, "http://127.0.0.1:11434");

        assertThat(response.status()).isEqualTo(ProviderCatalogStatus.UNAVAILABLE);
        assertThat(response.message()).doesNotContain("127.0.0.1");
    }

    private ProviderConfiguration provider(ProviderType providerType) {
        return ProviderConfiguration.builder()
                .id(providerId)
                .userId(userId)
                .providerType(providerType)
                .displayName("My provider")
                .endpoint("http://127.0.0.1:11434")
                .selectedModel("qwen3:8b")
                .enabled(true)
                .build();
    }
}
