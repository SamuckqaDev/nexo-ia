package com.nexoia.mcp.connection.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.mcp.catalog.dto.McpCatalogServerResponse;
import com.nexoia.mcp.catalog.service.McpCatalogService;
import com.nexoia.mcp.connection.dto.CreateRemoteMcpConnectionRequest;
import com.nexoia.mcp.connection.dto.McpConnectionResponse;
import com.nexoia.mcp.connection.dto.McpToolResponse;
import com.nexoia.mcp.connection.dto.UpdateMcpToolsRequest;
import com.nexoia.mcp.connection.exception.McpConnectionConflictException;
import com.nexoia.mcp.connection.exception.McpConnectionNotFoundException;
import com.nexoia.mcp.connection.exception.McpConnectionNotReadyException;
import com.nexoia.mcp.connection.exception.McpConfigurationNotSupportedException;
import com.nexoia.mcp.connection.exception.McpCredentialsNotSupportedException;
import com.nexoia.mcp.connection.exception.McpToolSelectionException;
import com.nexoia.mcp.connection.model.McpConnection;
import com.nexoia.mcp.connection.model.McpConnectionKind;
import com.nexoia.mcp.connection.model.McpConnectionStatus;
import com.nexoia.mcp.connection.model.McpCostType;
import com.nexoia.mcp.connection.model.McpToolDefinition;
import com.nexoia.mcp.connection.model.McpTransportType;
import com.nexoia.mcp.connection.repository.McpConnectionRepository;
import com.nexoia.mcp.connection.repository.McpToolDefinitionRepository;
import com.nexoia.mcp.runtime.dto.McpRuntimeConnection;
import com.nexoia.mcp.runtime.dto.McpRuntimeTool;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class McpConnectionService {

    private static final int MAX_ENABLED_CONNECTIONS = 4;
    private static final int MAX_ENABLED_TOOLS = 12;

    private final McpConnectionRepository connections;
    private final McpToolDefinitionRepository tools;
    private final McpCatalogService catalog;
    private final McpEndpointNormalizer endpoints;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<McpConnectionResponse> list(UUID userId) {
        return connections.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public McpConnectionResponse installDocker(UUID userId, String serverId) {
        McpCatalogServerResponse server = catalog.server(serverId);
        if (server.requiresSecrets()) {
            throw new McpCredentialsNotSupportedException();
        }
        if (server.requiresConfiguration()) {
            throw new McpConfigurationNotSupportedException();
        }
        if (connections.existsByUserIdAndCatalogServerId(userId, serverId)
                || connections.existsByUserIdAndDisplayNameIgnoreCase(userId, server.title())) {
            throw new McpConnectionConflictException();
        }
        McpConnection connection = connections.saveAndFlush(McpConnection.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .displayName(server.title())
                .connectionKind(McpConnectionKind.DOCKER_CATALOG)
                .transportType(McpTransportType.DOCKER_GATEWAY)
                .catalogServerId(server.id())
                .costType(server.costType())
                .status(McpConnectionStatus.PENDING)
                .enabled(false)
                .build());
        audit.record(RecordAuditCommand.success(
                AuditAction.MCP_CONNECTION_CREATED, userId, null,
                AuditTargetType.MCP_SERVER, connection.getId()));
        return response(connection);
    }

    @Transactional
    public McpConnectionResponse createRemote(UUID userId, CreateRemoteMcpConnectionRequest request) {
        String displayName = request.displayName().trim();
        if (connections.existsByUserIdAndDisplayNameIgnoreCase(userId, displayName)) {
            throw new McpConnectionConflictException();
        }
        McpConnection connection = connections.saveAndFlush(McpConnection.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .displayName(displayName)
                .connectionKind(McpConnectionKind.CUSTOM_REMOTE)
                .transportType(McpTransportType.STREAMABLE_HTTP)
                .endpoint(endpoints.normalize(request.endpoint()))
                .costType(McpCostType.UNKNOWN)
                .status(McpConnectionStatus.PENDING)
                .enabled(false)
                .build());
        audit.record(RecordAuditCommand.success(
                AuditAction.MCP_CONNECTION_CREATED, userId, null,
                AuditTargetType.MCP_SERVER, connection.getId()));
        return response(connection);
    }

    @Transactional
    public McpConnectionResponse setEnabled(UUID userId, UUID connectionId, boolean enabled) {
        McpConnection connection = owned(userId, connectionId);
        if (enabled && !connection.isEnabled()
                && connections.countByUserIdAndEnabledTrue(userId) >= MAX_ENABLED_CONNECTIONS) {
            throw new McpToolSelectionException("Enable at most four MCP connections at a time");
        }
        if (enabled && (connection.getStatus() != McpConnectionStatus.CONNECTED
                && connection.getStatus() != McpConnectionStatus.DISABLED)) {
            throw new McpConnectionNotReadyException();
        }
        if (enabled && tools.findAllByConnectionIdOrderByExternalNameAsc(connectionId).stream()
                .noneMatch(McpToolDefinition::isEnabled)) {
            throw new McpConnectionNotReadyException();
        }
        connection.setEnabled(enabled);
        connections.save(connection);
        audit.record(RecordAuditCommand.success(
                AuditAction.MCP_CONNECTION_UPDATED, userId, null,
                AuditTargetType.MCP_SERVER, connectionId));
        return response(connection);
    }

    @Transactional
    public McpConnectionResponse selectTools(
            UUID userId, UUID connectionId, UpdateMcpToolsRequest request) {
        McpConnection connection = owned(userId, connectionId);
        List<McpToolDefinition> available = tools.findAllByConnectionIdOrderByExternalNameAsc(connectionId);
        Set<String> selected = new HashSet<>(request.enabledToolNames());
        if (selected.size() > MAX_ENABLED_TOOLS) {
            throw new McpToolSelectionException("Select at most twelve MCP tools at a time");
        }
        Set<String> availableNames = available.stream()
                .map(McpToolDefinition::getExternalName)
                .collect(Collectors.toSet());
        if (selected.size() != request.enabledToolNames().size()
                || !availableNames.containsAll(selected)) {
            throw new McpToolSelectionException();
        }
        available.forEach(tool -> tool.setEnabled(selected.contains(tool.getExternalName())));
        tools.saveAll(available);
        if (selected.isEmpty() && connection.isEnabled()) {
            connection.setEnabled(false);
            connections.save(connection);
        }
        audit.record(RecordAuditCommand.success(
                AuditAction.MCP_CONNECTION_UPDATED, userId, null,
                AuditTargetType.MCP_SERVER, connectionId));
        return response(connection);
    }

    @Transactional
    public void remove(UUID userId, UUID connectionId) {
        McpConnection connection = owned(userId, connectionId);
        connections.delete(connection);
        audit.record(RecordAuditCommand.success(
                AuditAction.MCP_CONNECTION_REMOVED, userId, null,
                AuditTargetType.MCP_SERVER, connectionId));
    }

    @Transactional(readOnly = true)
    public McpRuntimeConnection runtimeConnection(UUID userId, UUID connectionId) {
        McpConnection connection = owned(userId, connectionId);
        return runtime(connection, List.of());
    }

    @Transactional(readOnly = true)
    public List<McpRuntimeConnection> enabledRuntimeConnections(UUID userId) {
        List<McpConnection> enabled = connections.findAllByUserIdAndEnabledTrueOrderByCreatedAtAsc(userId)
                .stream().limit(4).toList();
        if (enabled.isEmpty()) {
            return List.of();
        }
        List<McpToolDefinition> enabledTools = tools
                .findAllByConnectionIdInAndEnabledTrueOrderByExposedNameAsc(
                        enabled.stream().map(McpConnection::getId).toList())
                .stream().limit(12).toList();
        return enabled.stream()
                .map(connection -> runtime(
                        connection,
                        enabledTools.stream()
                                .filter(tool -> tool.getConnectionId().equals(connection.getId()))
                                .toList()))
                .filter(connection -> !connection.enabledTools().isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public McpConnectionResponse response(UUID userId, UUID connectionId) {
        return response(owned(userId, connectionId));
    }

    private McpRuntimeConnection runtime(McpConnection connection, List<McpToolDefinition> selected) {
        String endpoint = connection.getConnectionKind() == McpConnectionKind.CUSTOM_REMOTE
                ? endpoints.normalize(connection.getEndpoint())
                : null;
        return new McpRuntimeConnection(
                connection.getId(),
                connection.getDisplayName(),
                connection.getConnectionKind(),
                connection.getTransportType(),
                connection.getCatalogServerId(),
                endpoint,
                selected.stream()
                        .map(tool -> new McpRuntimeTool(tool.getExternalName(), tool.getExposedName()))
                        .toList());
    }

    private McpConnection owned(UUID userId, UUID connectionId) {
        return connections.findByIdAndUserId(connectionId, userId)
                .orElseThrow(McpConnectionNotFoundException::new);
    }

    private McpConnectionResponse response(McpConnection connection) {
        List<McpToolResponse> toolResponses = tools
                .findAllByConnectionIdOrderByExternalNameAsc(connection.getId()).stream()
                .map(tool -> new McpToolResponse(
                        tool.getExternalName(), tool.getExposedName(), tool.getTitle(), tool.getDescription(),
                        tool.isEnabled(), tool.getReadOnlyHint(), tool.getDestructiveHint(),
                        tool.getOpenWorldHint(), tool.getDiscoveredAt()))
                .toList();
        return new McpConnectionResponse(
                connection.getId(), connection.getDisplayName(), connection.getConnectionKind(),
                connection.getTransportType(), connection.getCatalogServerId(), connection.getEndpoint(),
                connection.getCostType(), connection.getStatus(), connection.isEnabled(),
                connection.getServerName(), connection.getServerVersion(), connection.getLastErrorCode(),
                connection.getLastConnectedAt(), toolResponses, connection.getCreatedAt(), connection.getUpdatedAt());
    }
}
