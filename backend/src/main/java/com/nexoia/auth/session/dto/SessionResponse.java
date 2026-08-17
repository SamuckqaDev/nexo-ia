package com.nexoia.auth.session.dto;

import com.nexoia.auth.session.model.SessionStatus;
import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        SessionStatus status,
        String initialIp,
        String lastIp,
        String userAgent,
        Instant createdAt,
        Instant lastSeenAt,
        Instant accessExpiresAt,
        Instant refreshExpiresAt,
        boolean current) {
}
