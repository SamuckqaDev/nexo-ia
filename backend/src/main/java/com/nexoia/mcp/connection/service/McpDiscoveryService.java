package com.nexoia.mcp.connection.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.mcp.connection.dto.McpConnectionResponse;
import com.nexoia.mcp.connection.exception.McpConnectionUnavailableException;
import com.nexoia.mcp.runtime.dto.McpRuntimeConnection;
import com.nexoia.mcp.runtime.service.McpClientFactory;
import com.nexoia.mcp.runtime.service.McpClientSession;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class McpDiscoveryService {

    private static final String CONNECTION_FAILED = "MCP_CONNECTION_FAILED";
    private final McpConnectionService connections;
    private final McpConnectionStore store;
    private final McpClientFactory clients;
    private final AuditService audit;

    public McpConnectionResponse discover(UUID userId, UUID connectionId) {
        McpRuntimeConnection connection = connections.runtimeConnection(userId, connectionId);
        try (McpClientSession session = clients.open(connection)) {
            store.replaceDiscovery(userId, connectionId, session.snapshot());
            audit.record(RecordAuditCommand.success(
                    AuditAction.MCP_CONNECTION_DISCOVERED, userId, null,
                    AuditTargetType.MCP_SERVER, connectionId));
            return connections.response(userId, connectionId);
        } catch (McpConnectionUnavailableException exception) {
            store.markUnavailable(userId, connectionId, CONNECTION_FAILED);
            throw exception;
        }
    }
}
