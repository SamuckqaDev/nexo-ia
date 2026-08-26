package com.nexoia.team.dto;

import com.nexoia.permission.model.ProfileKey;
import com.nexoia.team.model.TeamRole;
import java.time.Instant;
import java.util.UUID;

public record TeamResponse(
        UUID id,
        String name,
        UUID createdBy,
        ProfileKey defaultProfile,
        Long tokenBudgetLimit,
        TeamRole teamRole,
        ProfileKey assignedProfile,
        boolean manageable,
        Instant createdAt,
        Instant updatedAt) {
}
