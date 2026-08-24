package com.nexoia.mcp.runtime.dto;

import java.util.List;

public record McpConnectionSnapshot(
        String serverName,
        String serverVersion,
        List<McpDiscoveredTool> tools) {}
