package com.nexoia.media.image.dto;

import com.nexoia.media.image.model.ImageGenerationStatus;
import java.time.Instant;
import java.util.UUID;

public record ImageGenerationResponse(
        UUID id,
        UUID conversationId,
        String prompt,
        ImageGenerationStatus status,
        String provider,
        String model,
        Integer progress,
        Integer etaSeconds,
        String errorCode,
        String contentUrl,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt) {}
