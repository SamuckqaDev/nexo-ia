package com.nexoia.provider.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionMessage;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.exception.ProviderStreamException;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.model.TokenSource;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/**
 * Exercises the Ollama protocol against a deterministic local fake, so streaming, cancellation, and
 * token accounting are verified without depending on an installed model.
 */
class OllamaChatCompletionClientTest {

    private static final String STREAM = """
            {"model":"qwen3:8b","message":{"role":"assistant","content":"Hel"},"done":false}
            {"model":"qwen3:8b","message":{"role":"assistant","content":"lo"},"done":false}
            {"model":"qwen3:8b","message":{"role":"assistant","content":""},"done":true,\
            "done_reason":"stop","prompt_eval_count":20,"eval_count":3}
            """;
    private static final String THINKING_STREAM = """
            {"model":"qwen3:8b","message":{"role":"assistant","content":"",\
            "thinking":"Check"},"done":false}
            {"model":"qwen3:8b","message":{"role":"assistant","content":"",\
            "thinking":"ing"},"done":false}
            {"model":"qwen3:8b","message":{"role":"assistant","content":"Done",\
            "thinking":""},"done":false}
            {"model":"qwen3:8b","message":{"role":"assistant","content":"",\
            "thinking":""},"done":true,"done_reason":"stop","prompt_eval_count":20,"eval_count":6}
            """;

    private HttpServer server;
    private OllamaChatCompletionClient client;
    private final AtomicReference<String> requestBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        client = new OllamaChatCompletionClient(RestClient.builder(), JsonMapper.builder().build());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void streamsEveryDeltaAndReportsProviderTokenCounts() {
        serve(STREAM);
        StringBuilder streamed = new StringBuilder();

        ChatCompletionOutcome outcome =
                client.stream(command(false), delta -> { }, streamed::append, () -> false);

        assertThat(streamed.toString()).isEqualTo("Hello");
        assertThat(outcome.content()).isEqualTo("Hello");
        assertThat(outcome.inputTokens()).isEqualTo(20);
        assertThat(outcome.outputTokens()).isEqualTo(3);
        assertThat(outcome.tokenSource()).isEqualTo(TokenSource.PROVIDER);
        assertThat(outcome.cancelled()).isFalse();
        assertThat(outcome.doneReason()).isEqualTo("stop");
        assertThat(requestBody.get()).contains("\"think\":false");
    }

    @Test
    void separatesThinkingFromThePersistableAnswerWhenEnabled() {
        serve(THINKING_STREAM);
        StringBuilder thinking = new StringBuilder();
        StringBuilder streamed = new StringBuilder();

        ChatCompletionOutcome outcome =
                client.stream(command(true), thinking::append, streamed::append, () -> false);

        assertThat(thinking.toString()).isEqualTo("Checking");
        assertThat(streamed.toString()).isEqualTo("Done");
        assertThat(outcome.content()).isEqualTo("Done");
        assertThat(outcome.content()).doesNotContain("Checking");
        assertThat(requestBody.get()).contains("\"think\":true");
    }

    @Test
    void stopsReadingAndKeepsThePartialAnswerWhenCancelled() {
        serve(STREAM);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        StringBuilder streamed = new StringBuilder();

        ChatCompletionOutcome outcome = client.stream(command(false), delta -> { }, delta -> {
            streamed.append(delta);
            cancelled.set(true);
        }, cancelled::get);

        assertThat(outcome.cancelled()).isTrue();
        assertThat(outcome.content()).isEqualTo("Hel");
        assertThat(outcome.inputTokens()).isNull();
        assertThat(outcome.tokenSource()).isNull();
    }

    @Test
    void reportsAProviderFailureWithoutLeakingTheEndpoint() {
        server.createContext("/api/chat", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        assertThatThrownBy(() -> client.stream(command(false), delta -> { }, delta -> { }, () -> false))
                .isInstanceOf(ProviderStreamException.class)
                .hasMessage("The model provider could not complete this request");
    }

    @Test
    void supportsOnlyTheOllamaProviderType() {
        assertThat(client.supports(ProviderType.OLLAMA)).isTrue();
        assertThat(client.supports(ProviderType.OPENAI)).isFalse();
        assertThat(client.supports(ProviderType.ANTHROPIC)).isFalse();
    }

    private void serve(String body) {
        server.createContext("/api/chat", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/x-ndjson");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
    }

    private ChatCompletionCommand command(boolean thinkingEnabled) {
        return new ChatCompletionCommand(
                ProviderType.OLLAMA,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "qwen3:8b",
                List.of(new ChatCompletionMessage("user", "hi")),
                thinkingEnabled);
    }
}
