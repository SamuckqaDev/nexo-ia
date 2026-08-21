package com.nexoia.knowledge.embedding.service;

import com.nexoia.knowledge.embedding.config.KnowledgeEmbeddingProperties;
import com.nexoia.knowledge.embedding.dto.EmbeddingCommand;
import com.nexoia.knowledge.embedding.dto.EmbeddingOutcome;
import com.nexoia.knowledge.embedding.exception.EmbeddingProviderUnavailableException;
import com.nexoia.provider.model.ProviderConfiguration;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.repository.ProviderConfigurationRepository;
import com.nexoia.provider.service.ProviderEndpointGuard;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the caller's own enabled Ollama provider and dispatches to the matching
 * {@link EmbeddingClient}, exactly the way {@code ModelRequestService} resolves a
 * {@code ChatCompletionClient} — embeddings reuse the same per-user Provider Registry as chat, never a
 * separate embedding-provider registry, and are never required for ordinary chat.
 */
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final List<EmbeddingClient> clients;
    private final ProviderConfigurationRepository providers;
    private final ProviderEndpointGuard endpointGuard;
    private final KnowledgeEmbeddingProperties properties;

    @Transactional(readOnly = true)
    public EmbeddingOutcome embed(UUID userId, List<String> inputs) {
        ProviderConfiguration provider = providers
                .findFirstByUserIdAndProviderTypeAndEnabledTrueOrderByCreatedAtAsc(userId, ProviderType.OLLAMA)
                .orElseThrow(EmbeddingProviderUnavailableException::new);
        endpointGuard.verify(provider.getProviderType(), provider.getEndpoint());

        EmbeddingClient client = clients.stream()
                .filter(candidate -> candidate.supports(provider.getProviderType()))
                .findFirst()
                .orElseThrow(EmbeddingProviderUnavailableException::new);

        return client.embed(new EmbeddingCommand(
                provider.getProviderType(), provider.getEndpoint(), properties.modelIdentifier(), inputs));
    }
}
