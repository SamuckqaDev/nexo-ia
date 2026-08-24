package com.nexoia.mcp.catalog.dto;

import com.nexoia.mcp.connection.model.McpCostType;

public record McpCatalogServerResponse(
        String id,
        String title,
        String description,
        String category,
        String image,
        String iconUrl,
        String license,
        McpCostType costType,
        McpRiskLevel riskLevel,
        boolean requiresSecrets,
        boolean requiresConfiguration,
        int toolCount,
        boolean recommended) {}
