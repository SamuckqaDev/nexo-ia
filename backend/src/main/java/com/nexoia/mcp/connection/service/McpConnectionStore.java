package com.nexoia.mcp.connection.service;

import com.nexoia.mcp.connection.exception.McpConnectionNotFoundException;
import com.nexoia.mcp.connection.model.McpConnection;
import com.nexoia.mcp.connection.model.McpToolDefinition;
import com.nexoia.mcp.connection.repository.McpConnectionRepository;
import com.nexoia.mcp.connection.repository.McpToolDefinitionRepository;
import com.nexoia.mcp.runtime.dto.McpConnectionSnapshot;
import com.nexoia.mcp.runtime.dto.McpDiscoveredTool;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Short MCP persistence transactions kept separate from network and process lifecycle work. */
@Service
@RequiredArgsConstructor
public class McpConnectionStore {

    private final McpConnectionRepository connections;
    private final McpToolDefinitionRepository tools;
    private final Clock clock;

    @Transactional
    public void replaceDiscovery(UUID userId, UUID connectionId, McpConnectionSnapshot snapshot) {
        McpConnection connection = owned(userId, connectionId);
        Set<String> previouslyEnabled = tools.findAllByConnectionIdOrderByExternalNameAsc(connectionId)
                .stream()
                .filter(McpToolDefinition::isEnabled)
                .map(McpToolDefinition::getExternalName)
                .collect(Collectors.toSet());
        tools.deleteAllByConnectionId(connectionId);
        tools.flush();
        Instant discoveredAt = clock.instant();
        for (McpDiscoveredTool tool : snapshot.tools()) {
            tools.save(McpToolDefinition.builder()
                    .id(UUID.randomUUID())
                    .connectionId(connectionId)
                    .externalName(tool.name())
                    .exposedName(exposedName(connectionId, tool.name()))
                    .title(bounded(tool.title(), 200))
                    .description(bounded(tool.description(), 2000))
                    .inputSchema(tool.inputSchema())
                    .enabled(previouslyEnabled.contains(tool.name()))
                    .readOnlyHint(tool.readOnlyHint())
                    .destructiveHint(tool.destructiveHint())
                    .openWorldHint(tool.openWorldHint())
                    .discoveredAt(discoveredAt)
                    .build());
        }
        connection.markConnected(
                bounded(snapshot.serverName(), 160),
                bounded(snapshot.serverVersion(), 80),
                discoveredAt);
        connections.save(connection);
    }

    @Transactional
    public void markUnavailable(UUID userId, UUID connectionId, String errorCode) {
        McpConnection connection = owned(userId, connectionId);
        connection.markUnavailable(errorCode);
        connections.save(connection);
    }

    private McpConnection owned(UUID userId, UUID connectionId) {
        return connections.findByIdAndUserId(connectionId, userId)
                .orElseThrow(McpConnectionNotFoundException::new);
    }

    private String exposedName(UUID connectionId, String externalName) {
        String safe = externalName.toLowerCase()
                .replaceAll("[^a-z0-9_-]", "_")
                .replaceAll("_+", "_");
        String value = "mcp_" + connectionId.toString().substring(0, 8) + "_" + safe;
        return value.length() <= 160 ? value : value.substring(0, 160);
    }

    private String bounded(String value, int maximum) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maximum ? trimmed : trimmed.substring(0, maximum);
    }
}
