package com.nexoia.memory.personal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePersonalMemoryRequest(
        @NotBlank @Size(max = 1000) String content) {}
