package com.nexoia.media.image.dto;

public record ImageRuntimeResponse(
        String provider,
        boolean configured,
        boolean available,
        String model,
        String message) {}
