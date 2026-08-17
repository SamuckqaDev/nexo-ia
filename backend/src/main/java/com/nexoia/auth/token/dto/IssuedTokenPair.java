package com.nexoia.auth.token.dto;

import java.time.Instant;
import java.util.UUID;

public record IssuedTokenPair(
        String accessToken,
        UUID accessJti,
        Instant accessExpiresAt,
        String refreshToken,
        String refreshTokenHash,
        Instant refreshExpiresAt) {
}
