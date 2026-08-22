package com.nexoia.provider.springai;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Builds Spring AI provider objects for one already-authorized request.
 *
 * <p>Every Nexo user registers their own Ollama endpoint and model, so a single application-wide
 * model bean cannot serve them: the endpoint and options must be per request. This factory therefore
 * constructs a fresh {@link OllamaChatModel}/{@link OllamaEmbeddingModel} for each call instead of
 * mutating a shared instance, which is the only way two users' configurations cannot leak into each
 * other. Construction is cheap — no network call happens until the model is actually invoked — so a
 * cache is deliberately avoided until a measured need appears.
 *
 * <p>The endpoint reaching this factory has already passed the endpoint guard/normalizer; the factory
 * never re-derives it from model-provided data.
 */
@Component
@RequiredArgsConstructor
public class SpringAiModelFactory {

    private final RestClient.Builder restClientBuilder;
    private final ObservationRegistry observationRegistry;

    /**
     * Constructs a chat model bound to one user's endpoint and selected model. Thinking is switched
     * explicitly so a model that never advertises reasoning is not silently asked for it.
     */
    public OllamaChatModel chatModel(String endpoint, String model, boolean thinkingEnabled) {
        OllamaChatOptions.Builder options = OllamaChatOptions.builder();
        options.model(model);
        if (thinkingEnabled) {
            options.enableThinking();
        } else {
            options.disableThinking();
        }

        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi(endpoint))
                .options(options.build())
                .observationRegistry(observationRegistry)
                .build();
    }

    /** Constructs an embedding model bound to one user's endpoint and configured embedding model. */
    public OllamaEmbeddingModel embeddingModel(String endpoint, String model) {
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi(endpoint))
                .options(OllamaEmbeddingOptions.builder().model(model).build())
                .observationRegistry(observationRegistry)
                .build();
    }

    private OllamaApi ollamaApi(String endpoint) {
        // Clone the shared builder so per-request base URL/config never mutates the application bean.
        return OllamaApi.builder()
                .baseUrl(endpoint)
                .restClientBuilder(restClientBuilder.clone())
                .build();
    }
}
