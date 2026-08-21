package com.nexoia.knowledge.ingestion.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Splits normalized source text into bounded, overlapping chunks. Fixed sizing (no per-source
 * configuration) keeps chunk boundaries predictable across re-ingestion. See D-026.
 */
@Service
public class ChunkingService {

    private static final int CHUNK_SIZE = 1_200;
    private static final int CHUNK_OVERLAP = 200;
    private static final int MAX_CHUNKS = 300;
    private static final int ESTIMATED_CHARACTERS_PER_TOKEN = 4;

    public List<ChunkDraft> chunk(String normalizedText) {
        List<ChunkDraft> drafts = new ArrayList<>();
        if (normalizedText == null || normalizedText.isBlank()) {
            return drafts;
        }

        int start = 0;
        int ordinal = 0;
        while (start < normalizedText.length() && drafts.size() < MAX_CHUNKS) {
            int end = Math.min(start + CHUNK_SIZE, normalizedText.length());
            String content = normalizedText.substring(start, end);
            drafts.add(new ChunkDraft(ordinal++, content, estimateTokens(content)));

            if (end >= normalizedText.length()) {
                break;
            }
            start = end - CHUNK_OVERLAP;
        }

        return drafts;
    }

    private int estimateTokens(String content) {
        return Math.ceilDiv(content.length(), ESTIMATED_CHARACTERS_PER_TOKEN);
    }

    public record ChunkDraft(int ordinal, String content, int tokenEstimate) {
    }
}
