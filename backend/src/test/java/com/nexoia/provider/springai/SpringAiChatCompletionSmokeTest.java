package com.nexoia.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.conversation.inference.tool.AgentPlanToolFactory;
import com.nexoia.knowledge.retrieval.tool.KnowledgeSearchToolFactory;
import com.nexoia.mcp.runtime.service.McpToolSessionFactory;
import com.nexoia.memory.personal.tool.RememberToolFactory;
import com.nexoia.provider.dto.ChatCompletionMessage;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.model.TokenSource;
import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

/**
 * Proves streaming and token accounting through the Spring AI adapter against a real Ollama
 * installation.
 *
 * <p>Excluded from the default suite because it needs an installed model. Run it deliberately with
 * {@code ./mvnw test -Dgroups=ollama}, optionally overriding {@code NEXO_SMOKE_OLLAMA_URL} and
 * {@code NEXO_SMOKE_OLLAMA_MODEL}.
 */
@Tag("ollama")
class SpringAiChatCompletionSmokeTest {

    private final SpringAiChatCompletionClient client = new SpringAiChatCompletionClient(
            new SpringAiModelFactory(RestClient.builder(), ObservationRegistry.NOOP),
            new SpringAiMessageMapper(),
            Mockito.mock(KnowledgeSearchToolFactory.class),
            Mockito.mock(AgentPlanToolFactory.class),
            Mockito.mock(RememberToolFactory.class),
            Mockito.mock(McpToolSessionFactory.class),
            ObservationRegistry.NOOP);

    @Test
    void streamsARealCompletionAndReportsRealTokenCounts() {
        AtomicInteger deltas = new AtomicInteger();
        StringBuilder streamed = new StringBuilder();

        ChatCompletionOutcome outcome = client.stream(command(), delta -> { }, delta -> {
            deltas.incrementAndGet();
            streamed.append(delta);
        }, () -> false);

        assertThat(deltas.get()).isPositive();
        assertThat(streamed.toString()).isEqualTo(outcome.content());
        assertThat(outcome.content()).isNotBlank();
        assertThat(outcome.inputTokens()).isPositive();
        assertThat(outcome.outputTokens()).isPositive();
        assertThat(outcome.tokenSource()).isEqualTo(TokenSource.PROVIDER);
    }

    private ChatCompletionCommand command() {
        String url = System.getenv().getOrDefault("NEXO_SMOKE_OLLAMA_URL", "http://localhost:11434");
        String model = System.getenv().getOrDefault("NEXO_SMOKE_OLLAMA_MODEL", "qwen3:8b");
        return new ChatCompletionCommand(
                ProviderType.OLLAMA,
                url,
                model,
                List.of(new ChatCompletionMessage("user", "Reply with the single word: ready")),
                false);
    }
}
