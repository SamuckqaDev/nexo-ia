package com.nexoia.media.image.runtime;

import java.util.List;

public record ImageRuntimeHealth(
        boolean configured,
        boolean available,
        String model,
        List<String> models,
        String message) {}
