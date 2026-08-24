package com.nexoia.mcp.connection.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateMcpToolsRequest(
        @NotNull @Size(max = 24) List<@Size(max = 160) String> enabledToolNames) {}
