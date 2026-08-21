package com.nexoia.knowledge.ingestion.service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Extracts bounded, plain-text content from a supported source's raw bytes. Markdown and plain text
 * pass through as-is; JSON is re-serialized with bounded pretty-printing; CSV is bounded by row count.
 * Every format is capped at {@link #MAX_NORMALIZED_CHARACTERS} regardless of path. See D-026.
 */
@Service
@RequiredArgsConstructor
class SourceNormalizer {

    private static final int MAX_NORMALIZED_CHARACTERS = 512 * 1024;
    private static final int MAX_CSV_ROWS = 2_000;

    private final ObjectMapper objectMapper;

    String normalize(String extension, byte[] content) {
        String text = switch (extension) {
            case "json" -> prettyJson(content);
            case "csv" -> boundedCsv(content);
            default -> new String(content, StandardCharsets.UTF_8);
        };

        return text.length() > MAX_NORMALIZED_CHARACTERS ? text.substring(0, MAX_NORMALIZED_CHARACTERS) : text;
    }

    private String prettyJson(byte[] content) {
        JsonNode node = objectMapper.readTree(content);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

    private String boundedCsv(byte[] content) {
        StringBuilder bounded = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new StringReader(new String(content, StandardCharsets.UTF_8)))) {
            String line;
            int row = 0;
            while (row < MAX_CSV_ROWS && (line = reader.readLine()) != null) {
                bounded.append(line).append('\n');
                row++;
            }
        } catch (java.io.IOException exception) {
            throw new UncheckedIOException(exception);
        }

        return bounded.toString();
    }
}
