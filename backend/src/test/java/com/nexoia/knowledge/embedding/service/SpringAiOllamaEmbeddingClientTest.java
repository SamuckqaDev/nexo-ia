package com.nexoia.knowledge.embedding.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexoia.knowledge.embedding.dto.EmbeddingCommand;
import com.nexoia.knowledge.embedding.dto.EmbeddingOutcome;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.springai.SpringAiModelFactory;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SpringAiOllamaEmbeddingClientTest {

    private HttpServer server;
    private SpringAiOllamaEmbeddingClient client;
    private final AtomicReference<String> requestBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/embed", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] payload = ("{\"model\":\"nomic-embed-text\","
                    + "\"embeddings\":[[0.1,0.2],[0.3,0.4]]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
        client = new SpringAiOllamaEmbeddingClient(new SpringAiModelFactory(
                RestClient.builder(), ObservationRegistry.NOOP));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void batchesInputsThroughSpringAiAndPreservesVectorOrderAndDimensions() {
        EmbeddingOutcome outcome = client.embed(new EmbeddingCommand(
                ProviderType.OLLAMA,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "nomic-embed-text",
                List.of("first", "second")));

        assertThat(outcome.dimensions()).isEqualTo(2);
        assertThat(outcome.embeddings()).hasSize(2);
        assertThat(outcome.embeddings().get(0)).containsExactly(0.1f, 0.2f);
        assertThat(outcome.embeddings().get(1)).containsExactly(0.3f, 0.4f);
        assertThat(requestBody.get()).contains("\"input\":[\"first\",\"second\"]");
    }
}
