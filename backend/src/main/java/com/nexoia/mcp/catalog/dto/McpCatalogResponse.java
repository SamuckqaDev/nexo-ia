package com.nexoia.mcp.catalog.dto;

import java.time.Instant;
import java.util.List;

public record McpCatalogResponse(
        boolean dockerAvailable,
        String gatewayVersion,
        String source,
        Instant refreshedAt,
        List<McpCatalogServerResponse> servers) {}
