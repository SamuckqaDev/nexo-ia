package com.nexoia.provider.service;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.json.JsonMapper;
import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionMessage;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.model.TokenSource;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Proves streaming and token accounting against a real Ollama installation.
 *
 * <p>Excluded from the default suite because it needs an installed model. Run it deliberately with
 * {@code ./mvnw test -Dgroups=ollama}, optionally overriding {@code NEXO_SMOKE_OLLAMA_URL} and
 * {@code NEXO_SMOKE_OLLAMA_MODEL}.
 */
@Tag("ollama")
class OllamaChatCompletionSmokeTest {

    private final OllamaChatCompletionClient client =
            new OllamaChatCompletionClient(RestClient.builder(), JsonMapper.builder().build());

    @Test
    void streamsARealCompletionAndReportsRealTokenCounts() {
        AtomicInteger deltas = new AtomicInteger();
        StringBuilder streamed = new StringBuilder();

        ChatCompletionOutcome outcome = client.stream(command(), delta -> {
            deltas.incrementAndGet();
            streamed.append(delta);
        }, () -> false);

        assertThat(deltas.get()).isPositive();
        assertThat(streamed.toString()).isEqualTo(outcome.content());
        assertThat(outcome.content()).isNotBlank();
        assertThat(outcome.inputTokens()).isPositive();
        assertThat(outcome.outputTokens()).isPositive();
        assertThat(outcome.tokenSource()).isEqualTo(TokenSource.PROVIDER);
        assertThat(outcome.cancelled()).isFalse();
    }

    @Test
    void stopsARealGenerationOnCancellation() {
        StringBuilder streamed = new StringBuilder();

        ChatCompletionOutcome outcome = client.stream(
                new ChatCompletionCommand(ProviderType.OLLAMA, endpoint(), model(),
                        List.of(new ChatCompletionMessage("user", "Count slowly from 1 to 200."))),
                streamed::append,
                () -> streamed.length() > 0);

        assertThat(outcome.cancelled()).isTrue();
        assertThat(outcome.outputTokens()).isNull();
    }

    private ChatCompletionCommand command() {
        return new ChatCompletionCommand(ProviderType.OLLAMA, endpoint(), model(),
                List.of(new ChatCompletionMessage("user", "Reply with the single word: hello")));
    }

    private String endpoint() {
        String configured = System.getenv("NEXO_SMOKE_OLLAMA_URL");
        return configured == null ? "http://127.0.0.1:11434" : configured;
    }

    private String model() {
        String configured = System.getenv("NEXO_SMOKE_OLLAMA_MODEL");
        return configured == null ? "qwen3:8b" : configured;
    }
}
