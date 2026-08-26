package com.nexoia.media.image.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexoia.media.image.config.ComfyUiProperties;
import com.nexoia.media.image.exception.ImageModelUnavailableException;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class ComfyUiImageGenerationRuntimeTest {

    private HttpServer server;
    private AtomicReference<String> queuedWorkflow;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        queuedWorkflow = new AtomicReference<>();
        server.createContext("/models/checkpoints", exchange -> {
            byte[] body = "[\"model.safetensors\",\"medical.safetensors\"]"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/prompt", exchange -> {
            queuedWorkflow.set(new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "{\"prompt_id\":\"job-1\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/history/job-1", exchange -> {
            byte[] body = """
                    {"job-1":{"outputs":{"42":{"images":[{
                      "filename":"nexo.png","subfolder":"","type":"output"
                    }]}}}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/view", exchange -> {
            byte[] body = new byte[] {1, 2, 3, 4};
            exchange.getResponseHeaders().add("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void queuesTheOfficialComfyWorkflowAndReadsTheGeneratedArtifact() {
        ComfyUiImageGenerationRuntime runtime = runtime();
        AtomicReference<String> runtimeJob = new AtomicReference<>();
        AtomicReference<String> model = new AtomicReference<>();

        GeneratedImage generated = runtime.generate(
                "A connected neural knowledge graph",
                "medical.safetensors",
                (id, value) -> {
                    runtimeJob.set(id);
                    model.set(value);
                });

        assertThat(runtimeJob).hasValue("job-1");
        assertThat(model).hasValue("medical.safetensors");
        assertThat(generated.bytes()).containsExactly(1, 2, 3, 4);
        assertThat(generated.mediaType()).isEqualTo("image/png");
        assertThat(queuedWorkflow.get())
                .contains("\"client_id\"")
                .contains("\"CheckpointLoaderSimple\"")
                .contains("\"KSampler\"")
                .contains("medical.safetensors")
                .contains("A connected neural knowledge graph");
    }

    @Test
    void reportsTheRuntimeAndCheckpointAsAvailable() {
        ImageRuntimeHealth health = runtime().health();

        assertThat(health.configured()).isTrue();
        assertThat(health.available()).isTrue();
        assertThat(health.model()).isEqualTo("model.safetensors");
        assertThat(health.models())
                .containsExactly("model.safetensors", "medical.safetensors");
    }

    @Test
    void rejectsARequestedCheckpointThatIsNotInstalled() {
        assertThatThrownBy(() -> runtime().generate(
                "A medical study",
                "missing.safetensors",
                (id, model) -> {}))
                .isInstanceOf(ImageModelUnavailableException.class);
    }

    private ComfyUiImageGenerationRuntime runtime() {
        ComfyUiProperties properties = new ComfyUiProperties(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "",
                Duration.ofMillis(1),
                Duration.ofSeconds(1),
                Path.of("target", "test-images"));
        return new ComfyUiImageGenerationRuntime(properties, RestClient.builder());
    }
}
