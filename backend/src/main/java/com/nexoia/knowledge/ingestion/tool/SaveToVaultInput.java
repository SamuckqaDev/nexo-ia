package com.nexoia.knowledge.ingestion.tool;

/**
 * Model-facing input for {@code save_to_vault}. Only the note itself — the target Vault, owner, and
 * path are resolved from the server-set scope, never from the model.
 */
public record SaveToVaultInput(String title, String content) {
}
