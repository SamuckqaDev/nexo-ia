package com.nexoia.knowledge.embedding.service;

import com.nexoia.knowledge.embedding.dto.EmbeddingCommand;
import com.nexoia.knowledge.embedding.dto.EmbeddingOutcome;
import com.nexoia.knowledge.embedding.exception.EmbeddingProviderUnavailableException;
import com.nexoia.provider.model.ProviderType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls Ollama's {@code POST /api/embed}, which accepts a batch of inputs and answers one embedding
 * vector per input in the same order. Read directly against the documented endpoint, matching
 * {@code OllamaChatCompletionClient}'s D-021 rationale.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaEmbeddingClient implements EmbeddingClient {

    private final RestClient.Builder restClientBuilder;

    @Override
    public boolean supports(ProviderType providerType) {
        return providerType == ProviderType.OLLAMA;
    }

    @Override
    public EmbeddingOutcome embed(EmbeddingCommand command) {
        try {
            OllamaEmbedResponse response = restClientBuilder.clone().baseUrl(command.endpoint()).build()
                    .post()
                    .uri("/api/embed")
                    .body(new OllamaEmbedRequest(command.model(), command.inputs()))
                    .retrieve()
                    .body(OllamaEmbedResponse.class);

            if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
                throw new EmbeddingProviderUnavailableException();
            }

            List<float[]> vectors = response.embeddings().stream().map(this::toFloatArray).toList();

            return new EmbeddingOutcome(vectors, command.model(), vectors.getFirst().length);
        } catch (RestClientException exception) {
            log.warn("[NEXO-BACK][KNOWLEDGE] Ollama embedding call failed model={} reason={}",
                    command.model(), exception.getClass().getSimpleName());
            throw new EmbeddingProviderUnavailableException();
        }
    }

    private float[] toFloatArray(List<Float> values) {
        float[] vector = new float[values.size()];
        for (int index = 0; index < values.size(); index++) {
            vector[index] = values.get(index);
        }

        return vector;
    }
}
