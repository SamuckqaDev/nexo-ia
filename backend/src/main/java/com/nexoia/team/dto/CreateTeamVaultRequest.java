package com.nexoia.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Creates a shared Knowledge Vault owned by a Team. */
public record CreateTeamVaultRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 500) String description) {
}
