package com.nexoia.media.image.runtime;

import java.util.function.BiConsumer;

public interface ImageGenerationRuntime {

    String provider();

    ImageRuntimeHealth health();

    GeneratedImage generate(String prompt, BiConsumer<String, String> onStarted);
}
