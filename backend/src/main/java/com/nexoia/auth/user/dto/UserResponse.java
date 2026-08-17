package com.nexoia.auth.user.dto;

import com.nexoia.auth.user.model.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String name,
        Instant createdAt,
        UserRole role) {
}
