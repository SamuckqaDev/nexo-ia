package com.nexoia.mcp.connection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRemoteMcpConnectionRequest(
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Size(max = 500) String endpoint) {}
