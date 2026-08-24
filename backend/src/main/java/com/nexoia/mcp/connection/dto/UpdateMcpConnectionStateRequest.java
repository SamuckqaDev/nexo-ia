package com.nexoia.mcp.connection.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateMcpConnectionStateRequest(@NotNull Boolean enabled) {}
