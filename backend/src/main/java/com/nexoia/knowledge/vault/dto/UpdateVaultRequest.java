package com.nexoia.knowledge.vault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateVaultRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 500) String description) {
}
