package com.nexoia.media.image.runtime;

import com.nexoia.media.image.config.ComfyUiProperties;
import com.nexoia.media.image.exception.ImageModelUnavailableException;
import com.nexoia.media.image.exception.ImageRuntimeUnavailableException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/** Local ComfyUI adapter using the official queue, history, and view HTTP routes. */
@Slf4j
@Component
public class ComfyUiImageGenerationRuntime implements ImageGenerationRuntime {

    private static final int WIDTH = 512;
    private static final int HEIGHT = 512;
    private static final int STEPS = 20;
    private static final int MAX_IMAGE_BYTES = 25 * 1024 * 1024;
    private static final String NEGATIVE_PROMPT =
            "low quality, blurry, malformed, watermark, text artifacts";

    private final ComfyUiProperties properties;
    private final RestClient.Builder restClientBuilder;

    public ComfyUiImageGenerationRuntime(
            ComfyUiProperties properties,
            RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public String provider() {
        return "COMFYUI";
    }

    @Override
    public ImageRuntimeHealth health() {
        if (!properties.configured()) {
            return new ImageRuntimeHealth(
                    false,
                    false,
                    null,
                    List.of(),
                    "Configure NEXO_COMFYUI_BASE_URL to enable local image generation.");
        }
        try {
            List<String> models = models(client());
            String checkpoint = checkpoint(models, null);
            return new ImageRuntimeHealth(
                    true,
                    true,
                    checkpoint,
                    models,
                    "ComfyUI is connected and has a checkpoint ready.");
        } catch (RuntimeException exception) {
            return new ImageRuntimeHealth(
                    true,
                    false,
                    null,
                    List.of(),
                    "ComfyUI could not be reached or has no checkpoint installed.");
        }
    }

    @Override
    public GeneratedImage generate(
            String prompt,
            String requestedModel,
            BiConsumer<String, String> onStarted) {
        if (!properties.configured()) {
            throw new ImageRuntimeUnavailableException();
        }
        try {
            RestClient client = client();
            String checkpoint = checkpoint(models(client), requestedModel);
            String clientId = UUID.randomUUID().toString();
            JsonNode queued = client.post()
                    .uri("/prompt")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "client_id", clientId,
                            "prompt", workflow(prompt, checkpoint)))
                    .retrieve()
                    .body(JsonNode.class);
            String promptId = queued == null ? "" : queued.path("prompt_id").asText("");
            if (promptId.isBlank()) {
                throw new ImageRuntimeUnavailableException();
            }
            onStarted.accept(promptId, checkpoint);
            OutputReference output = waitForOutput(client, promptId);
            ResponseEntity<byte[]> response = client.get()
                    .uri(uriBuilder -> uriBuilder.path("/view")
                            .queryParam("filename", output.filename())
                            .queryParam("subfolder", output.subfolder())
                            .queryParam("type", output.type())
                            .build())
                    .retrieve()
                    .toEntity(byte[].class);
            byte[] bytes = response.getBody();
            if (bytes == null || bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
                throw new ImageRuntimeUnavailableException();
            }
            String mediaType = response.getHeaders().getContentType() == null
                    ? "image/png"
                    : response.getHeaders().getContentType().toString();
            return new GeneratedImage(promptId, checkpoint, output.filename(), mediaType, bytes);
        } catch (ImageModelUnavailableException | ImageRuntimeUnavailableException exception) {
            throw exception;
        } catch (RestClientException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("[NEXO-BACK][MEDIA] ComfyUI generation failed reason={}",
                    exception.getClass().getSimpleName());
            throw new ImageRuntimeUnavailableException(exception);
        }
    }

    private RestClient client() {
        return restClientBuilder.clone().baseUrl(properties.baseUrl()).build();
    }

    private List<String> models(RestClient client) {
        JsonNode response = client.get()
                .uri("/models/checkpoints")
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.isArray() || response.isEmpty()) {
            throw new ImageRuntimeUnavailableException();
        }
        List<String> models = response.valueStream()
                .map(JsonNode::asText)
                .filter(model -> !model.isBlank())
                .distinct()
                .toList();
        if (models.isEmpty()) {
            throw new ImageRuntimeUnavailableException();
        }
        return models;
    }

    private String checkpoint(List<String> models, String requestedModel) {
        if (requestedModel != null && !requestedModel.isBlank()) {
            return models.stream()
                    .filter(requestedModel::equals)
                    .findFirst()
                    .orElseThrow(ImageModelUnavailableException::new);
        }
        if (!properties.checkpoint().isBlank()) {
            return models.stream()
                    .filter(properties.checkpoint()::equals)
                    .findFirst()
                    .orElseThrow(ImageRuntimeUnavailableException::new);
        }
        return models.getFirst();
    }

    private OutputReference waitForOutput(RestClient client, String promptId)
            throws InterruptedException {
        Instant deadline = Instant.now().plus(properties.timeout());
        while (Instant.now().isBefore(deadline)) {
            JsonNode history = client.get()
                    .uri("/history/{promptId}", promptId)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode entry = history == null ? null : history.path(promptId);
            if (entry != null && !entry.isMissingNode()) {
                for (JsonNode output : entry.path("outputs")) {
                    JsonNode images = output.path("images");
                    if (images.isArray() && !images.isEmpty()) {
                        JsonNode image = images.get(0);
                        return new OutputReference(
                                image.path("filename").asText(),
                                image.path("subfolder").asText(""),
                                image.path("type").asText("output"));
                    }
                }
                if ("error".equalsIgnoreCase(entry.path("status").path("status_str").asText())) {
                    throw new ImageRuntimeUnavailableException();
                }
            }
            Thread.sleep(properties.pollInterval());
        }
        throw new ImageRuntimeUnavailableException();
    }

    private Map<String, Object> workflow(String prompt, String checkpoint) {
        long seed = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
        Map<String, Object> sampler = new LinkedHashMap<>();
        sampler.put("seed", seed);
        sampler.put("steps", STEPS);
        sampler.put("cfg", 7.0);
        sampler.put("sampler_name", "euler");
        sampler.put("scheduler", "normal");
        sampler.put("denoise", 1.0);
        sampler.put("model", List.of("4", 0));
        sampler.put("positive", List.of("6", 0));
        sampler.put("negative", List.of("7", 0));
        sampler.put("latent_image", List.of("5", 0));

        Map<String, Object> workflow = new LinkedHashMap<>();
        workflow.put("3", node("KSampler", sampler));
        workflow.put("4", node("CheckpointLoaderSimple", Map.of("ckpt_name", checkpoint)));
        workflow.put("5", node("EmptyLatentImage", Map.of(
                "width", WIDTH, "height", HEIGHT, "batch_size", 1)));
        workflow.put("6", node("CLIPTextEncode", Map.of(
                "text", prompt, "clip", List.of("4", 1))));
        workflow.put("7", node("CLIPTextEncode", Map.of(
                "text", NEGATIVE_PROMPT, "clip", List.of("4", 1))));
        workflow.put("8", node("VAEDecode", Map.of(
                "samples", List.of("3", 0), "vae", List.of("4", 2))));
        workflow.put("9", node("SaveImage", Map.of(
                "filename_prefix", "nexo_" + System.currentTimeMillis(),
                "images", List.of("8", 0))));
        return workflow;
    }

    private Map<String, Object> node(String classType, Map<String, Object> inputs) {
        return Map.of("class_type", classType, "inputs", inputs);
    }

    URI endpoint() {
        return properties.configured() ? URI.create(properties.baseUrl()) : null;
    }

    private record OutputReference(String filename, String subfolder, String type) {}
}
