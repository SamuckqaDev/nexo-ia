package com.nexoia.team.dto;

import com.nexoia.permission.model.ProfileKey;
import com.nexoia.team.model.TeamRole;
import java.time.Instant;
import java.util.UUID;

public record TeamMemberResponse(
        UUID userId,
        TeamRole teamRole,
        ProfileKey assignedProfile,
        Instant joinedAt) {
}
