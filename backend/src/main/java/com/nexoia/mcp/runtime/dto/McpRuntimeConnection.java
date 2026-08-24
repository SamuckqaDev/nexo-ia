package com.nexoia.mcp.runtime.dto;

import com.nexoia.mcp.connection.model.McpConnectionKind;
import com.nexoia.mcp.connection.model.McpTransportType;
import java.util.List;
import java.util.UUID;

public record McpRuntimeConnection(
        UUID id,
        String displayName,
        McpConnectionKind connectionKind,
        McpTransportType transportType,
        String catalogServerId,
        String endpoint,
        List<McpRuntimeTool> enabledTools) {}
