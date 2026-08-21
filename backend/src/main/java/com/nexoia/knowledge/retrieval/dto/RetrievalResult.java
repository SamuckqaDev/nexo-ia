package com.nexoia.knowledge.retrieval.dto;

import java.util.List;

/**
 * An empty result is explicit and safe: no selected vault, no authorized vault, no chunk above the
 * minimum score, or an unavailable embedding provider all resolve here — never an invented answer and
 * never a lexical fallback in this release. See D-026.
 */
public record RetrievalResult(List<CitationResponse> citations) {

    public static RetrievalResult empty() {
        return new RetrievalResult(List.of());
    }

    public boolean hasCitations() {
        return !citations.isEmpty();
    }
}
