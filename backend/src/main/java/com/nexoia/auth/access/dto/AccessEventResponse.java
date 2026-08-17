package com.nexoia.auth.access.dto;

import com.nexoia.auth.access.model.AccessEventType;
import java.time.Instant;
import java.util.UUID;

public record AccessEventResponse(
        long id,
        UUID sessionId,
        AccessEventType eventType,
        boolean success,
        String ipAddress,
        String userAgent,
        Instant occurredAt) {
}
