package com.nexoia.knowledge.ingestion.dto;

import com.nexoia.knowledge.ingestion.model.SourceStatus;

public record SourceIngestionStatusResponse(
        SourceStatus status,
        String errorCode,
        int chunkCount,
        String contentHash,
        int byteSize,
        String mimeType) {
}
