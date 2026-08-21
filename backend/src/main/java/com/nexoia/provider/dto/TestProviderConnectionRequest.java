package com.nexoia.provider.dto;

import com.nexoia.provider.model.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TestProviderConnectionRequest(
        @NotNull ProviderType providerType,
        @NotBlank @Size(max = 500) String endpoint) {
}
