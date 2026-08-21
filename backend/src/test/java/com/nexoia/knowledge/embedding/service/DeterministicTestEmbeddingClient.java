package com.nexoia.knowledge.embedding.service;

import com.nexoia.knowledge.embedding.dto.EmbeddingCommand;
import com.nexoia.knowledge.embedding.dto.EmbeddingOutcome;
import com.nexoia.provider.model.ProviderType;
import java.util.List;
import java.util.Random;

/**
 * A hash-seeded, deterministic stand-in for {@link OllamaEmbeddingClient} — same input text always
 * produces the same vector, so isolation and ranking tests can assert on retrieval behavior without a
 * live Ollama instance. Test source set only; never a Spring bean in {@code src/main}. See D-026.
 */
public class DeterministicTestEmbeddingClient implements EmbeddingClient {

    private static final int DIMENSIONS = 768;

    @Override
    public boolean supports(ProviderType providerType) {
        return providerType == ProviderType.OLLAMA;
    }

    @Override
    public EmbeddingOutcome embed(EmbeddingCommand command) {
        List<float[]> vectors = command.inputs().stream().map(this::vectorFor).toList();

        return new EmbeddingOutcome(vectors, command.model(), DIMENSIONS);
    }

    private float[] vectorFor(String text) {
        Random random = new Random(text.hashCode());
        float[] vector = new float[DIMENSIONS];
        for (int index = 0; index < DIMENSIONS; index++) {
            vector[index] = random.nextFloat() * 2 - 1;
        }

        return vector;
    }
}
