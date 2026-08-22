package com.nexoia.conversation.inference.context;

/**
 * A safe, model-facing summary of one retrieved source. Carries display labels and the chunk ordinal
 * only — never vectors, absolute paths, ids the model could weaponize, or the full source body.
 */
public record ContextSourceSummary(String vaultName, String sourceDisplayName, int chunkOrdinal) {}
