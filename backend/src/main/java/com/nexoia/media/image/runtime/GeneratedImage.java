package com.nexoia.media.image.runtime;

public record GeneratedImage(
        String runtimeJobId,
        String model,
        String filename,
        String mediaType,
        byte[] bytes) {}
