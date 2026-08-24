package com.nexoia.mcp.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.mcp.connection.model.McpConnectionKind;
import com.nexoia.mcp.connection.model.McpTransportType;
import com.nexoia.mcp.runtime.dto.McpConnectionSnapshot;
import com.nexoia.mcp.runtime.dto.McpRuntimeConnection;
import com.nexoia.mcp.runtime.dto.McpRuntimeTool;
import com.nexoia.provider.dto.McpToolScope;
import com.nexoia.provider.dto.ToolExecutionObserver;
import io.modelcontextprotocol.client.McpSyncClient;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

class McpToolSessionFactoryTest {

    @Test
    void governsExternalCallbacksAndDeniesRepeatedArguments() {
        McpClientFactory clients = mock(McpClientFactory.class);
        AuditService audit = mock(AuditService.class);
        ToolExecutionObserver observer = mock(ToolExecutionObserver.class);
        ToolCallback delegate = mock(ToolCallback.class);
        ToolDefinition definition = ToolDefinition.builder()
                .name("mcp_12345678_fetch")
                .description("Fetch a page")
                .inputSchema("{\"type\":\"object\"}")
                .build();
        when(delegate.getToolDefinition()).thenReturn(definition);
        when(delegate.call("{\"url\":\"https://example.com\"}"))
                .thenReturn("page content");

        UUID connectionId = UUID.randomUUID();
        McpRuntimeConnection connection = new McpRuntimeConnection(
                connectionId, "Fetch", McpConnectionKind.DOCKER_CATALOG,
                McpTransportType.DOCKER_GATEWAY, "fetch", null,
                List.of(new McpRuntimeTool("fetch", "mcp_12345678_fetch")));
        McpSyncClient client = mock(McpSyncClient.class);
        when(clients.open(connection)).thenReturn(new McpClientSession(
                client, new McpConnectionSnapshot("fetch", "1", List.of()), List.of(delegate)));
        McpToolScope scope = new McpToolScope(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), List.of(connection));
        McpToolSessionFactory factory = new McpToolSessionFactory(
                clients, audit,
                Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneOffset.UTC));

        try (McpToolSession session = factory.open(scope, observer, () -> false)) {
            ToolCallback callback = session.callbacks().getFirst();
            assertThat(callback.call("{\"url\":\"https://example.com\"}"))
                    .isEqualTo("page content");
            assertThat(callback.call("{\"url\":\"https://example.com\"}"))
                    .contains("denied");
            assertThat(session.evidence()).extracting(value -> value.status().name())
                    .containsExactly("COMPLETED", "DENIED");
        }

        verify(client).closeGracefully();
    }
}
