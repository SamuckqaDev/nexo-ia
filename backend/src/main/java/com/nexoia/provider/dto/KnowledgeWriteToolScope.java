package com.nexoia.provider.dto;

import java.util.UUID;

/**
 * Server-resolved scope for the governed {@code save_to_vault} tool. The writable Vault is chosen and
 * authorized outside the model; the model-facing schema never carries a Vault id, owner id, or path.
 */
public record KnowledgeWriteToolScope(
        UUID userId,
        UUID vaultId,
        String vaultName,
        UUID assistantMessageId,
        UUID correlationId) {

    /** Whether a writable Vault target was resolved for this request. */
    public boolean available() {
        return vaultId != null;
    }
}
