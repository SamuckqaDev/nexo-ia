package com.nexoia.mcp.connection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.mcp.connection.model.McpConnection;
import com.nexoia.mcp.connection.model.McpConnectionKind;
import com.nexoia.mcp.connection.model.McpConnectionStatus;
import com.nexoia.mcp.connection.model.McpCostType;
import com.nexoia.mcp.connection.model.McpToolDefinition;
import com.nexoia.mcp.connection.model.McpTransportType;
import com.nexoia.mcp.connection.repository.McpConnectionRepository;
import com.nexoia.mcp.connection.repository.McpToolDefinitionRepository;
import com.nexoia.mcp.gateway.service.DockerMcpGatewayRegistry;
import com.nexoia.mcp.runtime.dto.McpConnectionSnapshot;
import com.nexoia.mcp.runtime.dto.McpDiscoveredTool;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class McpConnectionStoreTest {

    private static final Instant NOW = Instant.parse("2026-09-11T12:00:00Z");

    @Mock private McpConnectionRepository connections;
    @Mock private McpToolDefinitionRepository tools;

    private McpConnectionStore store;

    @BeforeEach
    void setUp() {
        store = new McpConnectionStore(connections, tools, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void firstMachineProfileDiscoveryMakesEverySafeToolEligibleWithoutBypassingTheConnectionGate() {
        UUID userId = UUID.randomUUID();
        McpConnection connection = machineProfile(userId, McpConnectionStatus.PENDING, false, null);
        when(connections.findByIdAndUserId(connection.getId(), userId)).thenReturn(Optional.of(connection));
        when(tools.findAllByConnectionIdOrderByExternalNameAsc(connection.getId())).thenReturn(List.of());

        store.replaceDiscovery(userId, connection.getId(), snapshot());

        ArgumentCaptor<McpToolDefinition> captured = ArgumentCaptor.forClass(McpToolDefinition.class);
        verify(tools).save(captured.capture());
        assertThat(captured.getValue().isEnabled()).isTrue();
        assertThat(connection.isEnabled()).isFalse();
        assertThat(connection.getStatus()).isEqualTo(McpConnectionStatus.CONNECTED);
    }

    @Test
    void refreshingADisabledMachineProfileKeepsTheCatalogDisabledButToolsEligible() {
        UUID userId = UUID.randomUUID();
        McpConnection connection = machineProfile(
                userId, McpConnectionStatus.DISABLED, false, NOW.minusSeconds(60));
        when(connections.findByIdAndUserId(connection.getId(), userId)).thenReturn(Optional.of(connection));
        when(tools.findAllByConnectionIdOrderByExternalNameAsc(connection.getId())).thenReturn(List.of());

        store.replaceDiscovery(userId, connection.getId(), snapshot());

        ArgumentCaptor<McpToolDefinition> captured = ArgumentCaptor.forClass(McpToolDefinition.class);
        verify(tools).save(captured.capture());
        assertThat(captured.getValue().isEnabled()).isTrue();
        assertThat(connection.isEnabled()).isFalse();
    }

    private McpConnection machineProfile(
            UUID userId, McpConnectionStatus status, boolean enabled, Instant lastConnectedAt) {
        return McpConnection.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .displayName("Machine MCP profile")
                .connectionKind(McpConnectionKind.DOCKER_CATALOG)
                .transportType(McpTransportType.DOCKER_GATEWAY)
                .catalogServerId(DockerMcpGatewayRegistry.MACHINE_PROFILE_SERVER_ID)
                .costType(McpCostType.LOCAL_FREE)
                .status(status)
                .enabled(enabled)
                .lastConnectedAt(lastConnectedAt)
                .build();
    }

    private McpConnectionSnapshot snapshot() {
        return new McpConnectionSnapshot(
                "Docker MCP Gateway",
                "2.0.1",
                List.of(new McpDiscoveredTool(
                        "fetch", "Fetch", "Fetch a URL", Map.of(), true, false, true)));
    }
}
