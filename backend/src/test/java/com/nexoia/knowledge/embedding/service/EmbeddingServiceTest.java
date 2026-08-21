package com.nexoia.knowledge.embedding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nexoia.knowledge.embedding.config.KnowledgeEmbeddingProperties;
import com.nexoia.knowledge.embedding.exception.EmbeddingProviderUnavailableException;
import com.nexoia.knowledge.embedding.dto.EmbeddingOutcome;
import com.nexoia.provider.model.ProviderConfiguration;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.repository.ProviderConfigurationRepository;
import com.nexoia.provider.service.ProviderEndpointGuard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private ProviderConfigurationRepository providers;
    @Mock
    private ProviderEndpointGuard endpointGuard;
    private EmbeddingService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new EmbeddingService(
                List.of(new DeterministicTestEmbeddingClient()), providers, endpointGuard,
                new KnowledgeEmbeddingProperties("nomic-embed-text"));
    }

    @Test
    void embedsThroughTheCallersOwnEnabledOllamaProvider() {
        when(providers.findFirstByUserIdAndProviderTypeAndEnabledTrueOrderByCreatedAtAsc(userId, ProviderType.OLLAMA))
                .thenReturn(Optional.of(ProviderConfiguration.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .providerType(ProviderType.OLLAMA)
                        .displayName("Local Ollama")
                        .endpoint("http://localhost:11434")
                        .enabled(true)
                        .build()));

        EmbeddingOutcome outcome = service.embed(userId, List.of("hello world"));

        assertThat(outcome.embeddings()).hasSize(1);
        assertThat(outcome.dimensions()).isEqualTo(768);
        assertThat(outcome.model()).isEqualTo("nomic-embed-text");
    }

    @Test
    void failsExplicitlyWhenTheCallerHasNoEnabledOllamaProvider() {
        when(providers.findFirstByUserIdAndProviderTypeAndEnabledTrueOrderByCreatedAtAsc(userId, ProviderType.OLLAMA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.embed(userId, List.of("hello")))
                .isInstanceOf(EmbeddingProviderUnavailableException.class);
    }
}
