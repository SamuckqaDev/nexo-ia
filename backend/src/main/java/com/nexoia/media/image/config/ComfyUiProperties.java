package com.nexoia.media.image.config;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("nexo.media.image.comfyui")
public record ComfyUiProperties(
        String baseUrl,
        String checkpoint,
        Duration pollInterval,
        Duration timeout,
        Path outputDirectory) {

    public ComfyUiProperties {
        baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        checkpoint = checkpoint == null ? "" : checkpoint.trim();
        pollInterval = pollInterval == null ? Duration.ofSeconds(1) : pollInterval;
        timeout = timeout == null ? Duration.ofMinutes(10) : timeout;
        outputDirectory = outputDirectory == null
                ? Path.of(".nexo-data", "media", "images")
                : outputDirectory;
    }

    public boolean configured() {
        return !baseUrl.isBlank();
    }
}
