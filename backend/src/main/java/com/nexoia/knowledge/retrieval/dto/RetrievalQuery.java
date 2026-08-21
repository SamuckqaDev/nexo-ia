package com.nexoia.knowledge.retrieval.dto;

import java.util.List;
import java.util.UUID;

/**
 * An internal service request, not an API DTO — {@code vaultIds} is intersected with the caller's own
 * authorized vaults before any vector search runs.
 */
public record RetrievalQuery(List<UUID> vaultIds, String text) {
}
