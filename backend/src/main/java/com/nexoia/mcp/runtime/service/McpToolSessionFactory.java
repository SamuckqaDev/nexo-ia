package com.nexoia.mcp.runtime.service;

import com.nexoia.audit.service.AuditService;
import com.nexoia.mcp.connection.exception.McpConnectionUnavailableException;
import com.nexoia.mcp.runtime.dto.McpRuntimeConnection;
import com.nexoia.provider.dto.McpToolScope;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.dto.ToolExecutionObserver;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/** Opens only server-side authorized MCP connections for the lifetime of one Agent request. */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpToolSessionFactory {

    public static final int MAX_CALLS = 6;

    private final McpClientFactory clients;
    private final AuditService audit;
    private final Clock clock;

    public McpToolSession open(
            McpToolScope scope,
            ToolExecutionObserver observer,
            BooleanSupplier cancelled) {
        List<McpClientSession> openedClients = new ArrayList<>();
        List<ToolCallback> callbacks = new ArrayList<>();
        List<ToolExecutionEvidence> evidence = new ArrayList<>();
        AtomicInteger totalCalls = new AtomicInteger();
        Set<String> seenCalls = new HashSet<>();

        for (McpRuntimeConnection connection : scope.connections()) {
            try {
                McpClientSession session = clients.open(connection);
                openedClients.add(session);
                session.callbacks().stream()
                        .map(callback -> (ToolCallback) new GovernedMcpToolCallback(
                                callback, scope, observer, cancelled, evidence,
                                totalCalls, seenCalls, audit, clock))
                        .forEach(callbacks::add);
            } catch (McpConnectionUnavailableException exception) {
                log.warn("[NEXO-BACK][MCP] Skipping unavailable request tool connectionId={}",
                        connection.id());
            }
        }
        return new McpToolSession(openedClients, callbacks, evidence);
    }
}
