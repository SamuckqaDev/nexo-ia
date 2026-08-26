package com.nexoia.media.image.dto;

import java.util.List;

public record ImageRuntimeResponse(
        String provider,
        boolean configured,
        boolean available,
        String model,
        List<String> models,
        String message) {}
