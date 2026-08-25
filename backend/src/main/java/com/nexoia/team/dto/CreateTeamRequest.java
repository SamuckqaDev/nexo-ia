package com.nexoia.team.dto;

import com.nexoia.permission.model.ProfileKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Creates a Team. The default profile is applied to members added without an explicit one. */
public record CreateTeamRequest(
        @NotBlank @Size(max = 120) String name,
        ProfileKey defaultProfile,
        Long tokenBudgetLimit) {
}
