package com.nexoia.mcp.connection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDockerMcpConnectionRequest(
        @NotBlank
        @Size(max = 120)
        @Pattern(regexp = "[a-z0-9][a-z0-9._-]*")
        String catalogServerId) {}
