package com.nexoia.knowledge.retrieval.dto;

/**
 * A safe, bounded reference to a retrieved chunk — vault and source names and an excerpt, never a raw
 * path, secret, or the full source. See D-026.
 */
public record CitationResponse(
        String vaultName, String sourceDisplayName, int chunkOrdinal, String excerpt, double score) {
}
