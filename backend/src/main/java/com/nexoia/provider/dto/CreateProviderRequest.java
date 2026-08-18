package com.nexoia.provider.dto;

import com.nexoia.provider.model.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProviderRequest(
        @NotNull ProviderType providerType,
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Size(max = 500) String endpoint,
        @Size(max = 160) String selectedModel) {
}
