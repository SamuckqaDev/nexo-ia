package com.nexoia.media.image.runtime;

import java.util.function.BiConsumer;

public interface ImageGenerationRuntime {

    String provider();

    ImageRuntimeHealth health();

    GeneratedImage generate(
            String prompt,
            String model,
            BiConsumer<String, String> onStarted);
}
