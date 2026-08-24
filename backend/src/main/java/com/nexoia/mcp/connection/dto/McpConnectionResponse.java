package com.nexoia.mcp.connection.dto;

import com.nexoia.mcp.connection.model.McpConnectionKind;
import com.nexoia.mcp.connection.model.McpConnectionStatus;
import com.nexoia.mcp.connection.model.McpCostType;
import com.nexoia.mcp.connection.model.McpTransportType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record McpConnectionResponse(
        UUID id,
        String displayName,
        McpConnectionKind connectionKind,
        McpTransportType transportType,
        String catalogServerId,
        String endpoint,
        McpCostType costType,
        McpConnectionStatus status,
        boolean enabled,
        String serverName,
        String serverVersion,
        String lastErrorCode,
        Instant lastConnectedAt,
        List<McpToolResponse> tools,
        Instant createdAt,
        Instant updatedAt) {}
