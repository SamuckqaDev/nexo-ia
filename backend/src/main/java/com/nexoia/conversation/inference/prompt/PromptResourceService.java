package com.nexoia.conversation.inference.prompt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Loads every mandatory prompt resource once at startup, trims it, and refuses to start when one is
 * missing or blank. Callers read the cached text; the resources are never re-read per request.
 */
@Service
public class PromptResourceService {

    private final Map<PromptResource, String> contents = new EnumMap<>(PromptResource.class);

    public PromptResourceService() {
        for (PromptResource resource : PromptResource.values()) {
            contents.put(resource, load(resource));
        }
    }

    public String get(PromptResource resource) {
        return contents.get(resource);
    }

    private String load(PromptResource resource) {
        ClassPathResource classpath = new ClassPathResource(resource.path());
        if (!classpath.exists()) {
            throw new MissingPromptResourceException(resource, "was not found on the classpath");
        }

        try {
            String text = new String(classpath.getContentAsByteArray(), StandardCharsets.UTF_8).strip();
            if (text.isBlank()) {
                throw new MissingPromptResourceException(resource, "is blank");
            }
            return text;
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read prompt resource " + resource.path(), exception);
        }
    }
}
