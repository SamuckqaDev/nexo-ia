package com.nexoia.mcp.runtime.dto;

import java.util.Map;

public record McpDiscoveredTool(
        String name,
        String title,
        String description,
        Map<String, Object> inputSchema,
        Boolean readOnlyHint,
        Boolean destructiveHint,
        Boolean openWorldHint) {}
