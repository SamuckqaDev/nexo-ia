package com.nexoia.team.dto;

import com.nexoia.permission.model.ProfileKey;
import com.nexoia.team.model.TeamRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Adds an existing user to a Team with a Team role and an assigned capability profile. */
public record AddTeamMemberRequest(
        @NotNull UUID userId,
        TeamRole teamRole,
        ProfileKey profile) {
}
