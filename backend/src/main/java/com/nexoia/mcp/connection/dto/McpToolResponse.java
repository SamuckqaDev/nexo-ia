package com.nexoia.mcp.connection.dto;

import java.time.Instant;

public record McpToolResponse(
        String name,
        String exposedName,
        String title,
        String description,
        boolean enabled,
        Boolean readOnlyHint,
        Boolean destructiveHint,
        Boolean openWorldHint,
        Instant discoveredAt) {}
