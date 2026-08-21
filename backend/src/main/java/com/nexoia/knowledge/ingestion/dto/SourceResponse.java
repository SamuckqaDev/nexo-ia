package com.nexoia.knowledge.ingestion.dto;

import com.nexoia.knowledge.ingestion.model.SourceKind;
import com.nexoia.knowledge.ingestion.model.SourceStatus;
import java.time.Instant;
import java.util.UUID;

public record SourceResponse(
        UUID id,
        UUID vaultId,
        SourceKind sourceKind,
        String displayName,
        String mimeType,
        int byteSize,
        SourceStatus status,
        String errorCode,
        Instant createdAt,
        Instant updatedAt) {
}
