package com.nexoia.media.image.runtime;

public record ImageRuntimeHealth(
        boolean configured,
        boolean available,
        String model,
        String message) {}
