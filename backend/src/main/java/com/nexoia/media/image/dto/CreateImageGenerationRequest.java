package com.nexoia.media.image.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateImageGenerationRequest(
        @NotBlank @Size(max = 4000) String prompt) {}
