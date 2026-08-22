package com.nexoia.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.client.RestClient;

/**
 * Proves the factory builds an isolated model per request: two users with different endpoints and
 * models never reuse each other's configuration, and a request reaches only its own endpoint.
 */
class SpringAiModelFactoryTest {

    private static final String DONE_STREAM = """
            {"model":"m","message":{"role":"assistant","content":"ok"},"done":true,\
            "done_reason":"stop","prompt_eval_count":1,"eval_count":1}
            """;

    private final SpringAiModelFactory factory =
            new SpringAiModelFactory(RestClient.builder(), ObservationRegistry.NOOP);

    private HttpServer serverA;
    private HttpServer serverB;
    private final AtomicReference<String> bodyA = new AtomicReference<>();
    private final AtomicReference<String> bodyB = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        serverA = start(bodyA);
        serverB = start(bodyB);
    }

    @AfterEach
    void tearDown() {
        serverA.stop(0);
        serverB.stop(0);
    }

    @Test
    void buildsADistinctChatModelCarryingEachUsersOwnModelAndThinkingOption() {
        OllamaChatModel alpha = factory.chatModel("http://x", "alpha", true);
        OllamaChatModel beta = factory.chatModel("http://y", "beta", false);

        assertThat(alpha).isNotSameAs(beta);

        OllamaChatOptions alphaOptions = alpha.getOptions();
        OllamaChatOptions betaOptions = beta.getOptions();
        assertThat(alphaOptions.getModel()).isEqualTo("alpha");
        assertThat(betaOptions.getModel()).isEqualTo("beta");
        assertThat(alphaOptions.getThinkOption().toJsonValue()).isEqualTo(Boolean.TRUE);
        assertThat(betaOptions.getThinkOption().toJsonValue()).isEqualTo(Boolean.FALSE);
    }

    @Test
    void routesEachUsersRequestOnlyToThatUsersEndpoint() {
        stream(factory.chatModel(endpoint(serverA), "alpha", false));
        stream(factory.chatModel(endpoint(serverB), "beta", false));

        assertThat(bodyA.get()).contains("\"model\":\"alpha\"");
        assertThat(bodyA.get()).doesNotContain("beta");
        assertThat(bodyB.get()).contains("\"model\":\"beta\"");
        assertThat(bodyB.get()).doesNotContain("alpha");
    }

    private void stream(OllamaChatModel model) {
        model.stream(new Prompt("hi")).collectList().block();
    }

    private String endpoint(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private HttpServer start(AtomicReference<String> capturedBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] payload = DONE_STREAM.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/x-ndjson");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
        return server;
    }
}
