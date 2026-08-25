package com.nexoia.knowledge.ingestion.tool;

import com.nexoia.provider.dto.ToolExecutionStatus;

/** Safe, bounded result returned to the model by {@code save_to_vault}. */
public record SaveToVaultResult(
        ToolExecutionStatus status,
        String vaultName,
        String message) {
}
