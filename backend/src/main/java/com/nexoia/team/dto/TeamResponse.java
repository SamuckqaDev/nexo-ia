package com.nexoia.team.dto;

import com.nexoia.permission.model.ProfileKey;
import java.time.Instant;
import java.util.UUID;

public record TeamResponse(
        UUID id,
        String name,
        UUID createdBy,
        ProfileKey defaultProfile,
        Long tokenBudgetLimit,
        Instant createdAt,
        Instant updatedAt) {
}
