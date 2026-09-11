package com.nexoia.provider.springai;

import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.secret.dto.ProviderAuthentication;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
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
public class SpringAiModelFactory {

    private final RestClient.Builder restClientBuilder;
    private final ObservationRegistry observationRegistry;

    @Autowired
    public SpringAiModelFactory(
            RestClient.Builder restClientBuilder,
            ObservationRegistry observationRegistry) {
        this.restClientBuilder = restClientBuilder;
        this.observationRegistry = observationRegistry;
    }

    /** Builds a request-local model using only the credential resolved by the server Secret Store. */
    public ChatModel chatModel(
            ProviderType providerType,
            String endpoint,
            String model,
            boolean thinkingEnabled,
            ProviderAuthentication authentication) {
        if (providerType == ProviderType.OLLAMA) {
            return chatModel(endpoint, model, thinkingEnabled);
        }
        if (providerType == ProviderType.ANTHROPIC) {
            return AnthropicChatModel.builder()
                    .options(AnthropicChatOptions.builder()
                            .apiKey(authentication.revealApiKey())
                            .baseUrl(endpoint)
                            .model(model)
                            .maxTokens(4096)
                            .build())
                    .observationRegistry(observationRegistry)
                    .build();
        }
        String apiKey = authentication.configured()
                ? authentication.revealApiKey()
                : "nexo-openai-compatible-no-auth";
        return OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .apiKey(apiKey)
                        .baseUrl(endpoint)
                        .model(model)
                        .build())
                .observationRegistry(observationRegistry)
                .build();
    }

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
