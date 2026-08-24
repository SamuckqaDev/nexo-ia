package com.nexoia.provider.dto;

import com.nexoia.mcp.runtime.dto.McpRuntimeConnection;
import java.util.List;
import java.util.UUID;

/** Server-resolved MCP servers and tools authorized for one Agent request. */
public record McpToolScope(
        UUID userId,
        UUID assistantMessageId,
        UUID correlationId,
        List<McpRuntimeConnection> connections) {

    public McpToolScope {
        connections = List.copyOf(connections);
    }

    public boolean available() {
        return !connections.isEmpty();
    }
}
