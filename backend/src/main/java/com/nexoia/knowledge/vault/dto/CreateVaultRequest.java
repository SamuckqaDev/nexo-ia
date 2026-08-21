package com.nexoia.knowledge.vault.dto;

import com.nexoia.knowledge.vault.model.VaultScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateVaultRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 500) String description,
        @NotNull VaultScope scope,
        UUID workspaceId) {
}
