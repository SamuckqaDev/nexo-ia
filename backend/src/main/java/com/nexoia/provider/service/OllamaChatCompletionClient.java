package com.nexoia.provider.service;

import tools.jackson.databind.ObjectMapper;
import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.dto.OllamaChatRequest;
import com.nexoia.provider.dto.OllamaChatStreamChunk;
import com.nexoia.provider.exception.ProviderStreamException;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.model.TokenSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Streams chat completions from an Ollama endpoint.
 *
 * <p>Ollama answers {@code POST /api/chat} with newline-delimited JSON: one line per content delta,
 * then a final line carrying {@code done}, the stop reason, and token accounting. Nexo IA reads that
 * stream directly rather than through a higher-level model abstraction, because each user registers
 * their own endpoint and because the release 0.1 backend is Spring MVC rather than reactive.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaChatCompletionClient implements ChatCompletionClient {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(ProviderType providerType) {
        return providerType == ProviderType.OLLAMA;
    }

    @Override
    public ChatCompletionOutcome stream(
            ChatCompletionCommand command,
            Consumer<String> onToken,
            BooleanSupplier cancelled) {
        OllamaChatRequest payload = new OllamaChatRequest(command.model(), command.messages(), true);

        try {
            return restClientBuilder.clone().baseUrl(command.endpoint()).build()
                    .post()
                    .uri("/api/chat")
                    .accept(MediaType.APPLICATION_NDJSON, MediaType.APPLICATION_JSON)
                    .body(payload)
                    .exchange((request, response) -> {
                        // exchange() hands over the raw response, so a provider error status must be
                        // rejected here; otherwise a failed generation would look like empty content.
                        if (response.getStatusCode().isError()) {
                            throw new ProviderStreamException(new IOException(
                                    "Provider answered status " + response.getStatusCode().value()));
                        }

                        return readStream(response.getBody(), onToken, cancelled);
                    });
        } catch (ProviderStreamException exception) {
            log.warn("[NEXO-BACK][PROVIDER] Ollama rejected the request model={}", command.model());
            throw exception;
        } catch (RestClientException | UncheckedIOException exception) {
            log.warn("[NEXO-BACK][PROVIDER] Ollama stream failed model={} reason={}",
                    command.model(), exception.getClass().getSimpleName());
            throw new ProviderStreamException(exception);
        }
    }

    private ChatCompletionOutcome readStream(
            java.io.InputStream body, Consumer<String> onToken, BooleanSupplier cancelled) {
        StringBuilder content = new StringBuilder();
        Integer inputTokens = null;
        Integer outputTokens = null;
        String doneReason = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (cancelled.getAsBoolean()) {
                    return new ChatCompletionOutcome(
                            content.toString(), null, null, null, true, "cancelled");
                }
                if (line.isBlank()) {
                    continue;
                }

                OllamaChatStreamChunk chunk =
                        objectMapper.readValue(line, OllamaChatStreamChunk.class);
                String delta = chunk.contentDelta();
                if (!delta.isEmpty()) {
                    content.append(delta);
                    onToken.accept(delta);
                }
                if (chunk.done()) {
                    inputTokens = chunk.promptEvalCount();
                    outputTokens = chunk.evalCount();
                    doneReason = chunk.doneReason();
                    break;
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }

        TokenSource tokenSource =
                inputTokens == null && outputTokens == null ? null : TokenSource.PROVIDER;

        return new ChatCompletionOutcome(
                content.toString(), inputTokens, outputTokens, tokenSource, false, doneReason);
    }
}
