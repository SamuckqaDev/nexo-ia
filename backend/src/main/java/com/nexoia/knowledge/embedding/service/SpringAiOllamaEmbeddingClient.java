package com.nexoia.knowledge.embedding.service;

import com.nexoia.knowledge.embedding.dto.EmbeddingCommand;
import com.nexoia.knowledge.embedding.dto.EmbeddingOutcome;
import com.nexoia.knowledge.embedding.exception.EmbeddingProviderUnavailableException;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.springai.SpringAiModelFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.stereotype.Service;

/**
 * Generates Ollama embeddings through Spring AI while retaining Nexo's provider-neutral boundary
 * and per-user endpoint isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpringAiOllamaEmbeddingClient implements EmbeddingClient {

    private final SpringAiModelFactory modelFactory;

    @Override
    public boolean supports(ProviderType providerType) {
        return providerType == ProviderType.OLLAMA;
    }

    @Override
    public EmbeddingOutcome embed(EmbeddingCommand command) {
        try {
            OllamaEmbeddingModel model = modelFactory.embeddingModel(command.endpoint(), command.model());
            EmbeddingResponse response = model.call(new EmbeddingRequest(command.inputs(), null));
            List<float[]> vectors = response.getResults().stream()
                    .sorted((left, right) -> left.getIndex().compareTo(right.getIndex()))
                    .map(Embedding::getOutput)
                    .toList();

            if (vectors.size() != command.inputs().size()
                    || vectors.isEmpty()
                    || vectors.stream().anyMatch(vector -> vector == null || vector.length == 0)
                    || vectors.stream().anyMatch(vector -> vector.length != vectors.getFirst().length)) {
                throw new EmbeddingProviderUnavailableException();
            }

            return new EmbeddingOutcome(vectors, command.model(), vectors.getFirst().length);
        } catch (EmbeddingProviderUnavailableException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("[NEXO-BACK][KNOWLEDGE] Spring AI embedding call failed model={} reason={}",
                    command.model(), exception.getClass().getSimpleName());
            throw new EmbeddingProviderUnavailableException();
        }
    }
}
