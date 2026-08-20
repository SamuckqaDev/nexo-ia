package com.nexoia.provider.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexoia.provider.exception.ProviderUnavailableException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OllamaProviderServiceTest {

    private HttpServer server;
    private OllamaProviderService service;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        service = new OllamaProviderService(RestClient.builder());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void readsTheOllamaTagsProtocolFromTheRegisteredEndpoint() {
        server.createContext("/api/tags", exchange -> {
            byte[] payload = """
                    {"models":[
                      {"name":"qwen3:8b","modified_at":"2026-08-20T12:00:00Z","size":123},
                      {"name":"nomic-embed-text","modified_at":null,"size":456}
                    ]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();

        var models = service.models(endpoint());

        assertThat(models).extracting(model -> model.name())
                .containsExactly("qwen3:8b", "nomic-embed-text");
        assertThat(models.getFirst().modifiedAt()).isNotNull();
    }

    @Test
    void hidesRemoteProtocolFailuresBehindASafeError() {
        server.createContext("/api/tags", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        assertThatThrownBy(() -> service.models(endpoint()))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasMessage("The Ollama provider is unavailable")
                .hasMessageNotContaining(endpoint());
    }

    private String endpoint() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }
}
