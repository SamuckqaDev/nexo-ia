package com.nexoia.mcp.connection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.mcp.catalog.dto.McpCatalogServerResponse;
import com.nexoia.mcp.catalog.dto.McpRiskLevel;
import com.nexoia.mcp.catalog.service.McpCatalogService;
import com.nexoia.mcp.connection.dto.UpdateMcpToolsRequest;
import com.nexoia.mcp.connection.dto.CreateRemoteMcpConnectionRequest;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class McpConnectionServiceTest {

    @Mock private McpConnectionRepository connections;
    @Mock private McpToolDefinitionRepository tools;
    @Mock private McpCatalogService catalog;
    @Mock private McpEndpointNormalizer endpoints;
    @Mock private AuditService audit;

    private McpConnectionService service;

    @BeforeEach
    void setUp() {
        service = new McpConnectionService(connections, tools, catalog, endpoints, audit);
    }

    @Test
    void blocksCatalogServersThatNeedSharedSecretsOrConfiguration() {
        UUID userId = UUID.randomUUID();
        when(catalog.server("secret")).thenReturn(server("secret", true, false));
        when(catalog.server("configured")).thenReturn(server("configured", false, true));

        assertThatThrownBy(() -> service.installDocker(userId, "secret"))
                .isInstanceOf(McpCredentialsNotSupportedException.class);
        assertThatThrownBy(() -> service.installDocker(userId, "configured"))
                .isInstanceOf(McpConfigurationNotSupportedException.class);
        verify(connections, never()).saveAndFlush(any());
    }

    @Test
    void flushesANewConnectionBeforeBuildingTheTimestampedResponse() {
        UUID userId = UUID.randomUUID();
        when(endpoints.normalize("https://tools.example.com/mcp"))
                .thenReturn("https://tools.example.com/mcp");
        when(connections.saveAndFlush(any(McpConnection.class)))
                .thenAnswer(call -> call.getArgument(0));

        service.createRemote(userId, new CreateRemoteMcpConnectionRequest(
                "My tools", "https://tools.example.com/mcp"));

        verify(connections).saveAndFlush(any(McpConnection.class));
    }

    @Test
    void rejectsToolsOutsideTheOwnedConnectionSnapshot() {
        UUID userId = UUID.randomUUID();
        McpConnection connection = connection(userId);
        when(connections.findByIdAndUserId(connection.getId(), userId)).thenReturn(Optional.of(connection));
        when(tools.findAllByConnectionIdOrderByExternalNameAsc(connection.getId()))
                .thenReturn(List.of(tool(connection.getId(), "fetch")));

        assertThatThrownBy(() -> service.selectTools(
                userId, connection.getId(), new UpdateMcpToolsRequest(List.of("filesystem"))))
                .isInstanceOf(McpToolSelectionException.class);
        verify(tools, never()).saveAll(any());
    }

    @Test
    void exposesOnlyEnabledToolsFromEnabledConnectionsOwnedByTheUser() {
        UUID userId = UUID.randomUUID();
        McpConnection connection = connection(userId);
        when(connections.findAllByUserIdAndEnabledTrueOrderByCreatedAtAsc(userId))
                .thenReturn(List.of(connection));
        McpToolDefinition enabled = tool(connection.getId(), "fetch");
        enabled.setEnabled(true);
        when(tools.findAllByConnectionIdInAndEnabledTrueOrderByExposedNameAsc(List.of(connection.getId())))
                .thenReturn(List.of(enabled));

        var runtime = service.enabledRuntimeConnections(userId);

        assertThat(runtime).singleElement().satisfies(value -> {
            assertThat(value.id()).isEqualTo(connection.getId());
            assertThat(value.enabledTools()).singleElement()
                    .satisfies(tool -> assertThat(tool.externalName()).isEqualTo("fetch"));
        });
    }

    @Test
    void rejectsMoreThanTwelveSelectedTools() {
        UUID userId = UUID.randomUUID();
        McpConnection connection = connection(userId);
        List<McpToolDefinition> available = IntStream.range(0, 13)
                .mapToObj(index -> tool(connection.getId(), "tool_" + index))
                .toList();
        when(connections.findByIdAndUserId(connection.getId(), userId)).thenReturn(Optional.of(connection));
        when(tools.findAllByConnectionIdOrderByExternalNameAsc(connection.getId())).thenReturn(available);

        assertThatThrownBy(() -> service.selectTools(
                userId, connection.getId(),
                new UpdateMcpToolsRequest(available.stream().map(McpToolDefinition::getExternalName).toList())))
                .isInstanceOf(McpToolSelectionException.class)
                .hasMessageContaining("twelve");
    }

    @Test
    void rejectsAFifthEnabledConnection() {
        UUID userId = UUID.randomUUID();
        McpConnection connection = connection(userId);
        connection.setEnabled(false);
        when(connections.findByIdAndUserId(connection.getId(), userId)).thenReturn(Optional.of(connection));
        when(connections.countByUserIdAndEnabledTrue(userId)).thenReturn(4L);

        assertThatThrownBy(() -> service.setEnabled(userId, connection.getId(), true))
                .isInstanceOf(McpToolSelectionException.class)
                .hasMessageContaining("four");
    }

    private McpCatalogServerResponse server(String id, boolean secrets, boolean configuration) {
        return new McpCatalogServerResponse(
                id, id, "description", "web", "mcp/" + id, null, "MIT",
                McpCostType.LOCAL_FREE, McpRiskLevel.READ_ONLY,
                secrets, configuration, 1, false);
    }

    private McpConnection connection(UUID userId) {
        return McpConnection.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .displayName("Fetch")
                .connectionKind(McpConnectionKind.DOCKER_CATALOG)
                .transportType(McpTransportType.DOCKER_GATEWAY)
                .catalogServerId("fetch")
                .costType(McpCostType.LOCAL_FREE)
                .status(McpConnectionStatus.CONNECTED)
                .enabled(true)
                .build();
    }

    private McpToolDefinition tool(UUID connectionId, String name) {
        return McpToolDefinition.builder()
                .id(UUID.randomUUID())
                .connectionId(connectionId)
                .externalName(name)
                .exposedName("mcp_12345678_" + name)
                .inputSchema(Map.of())
                .enabled(false)
                .discoveredAt(Instant.parse("2026-08-24T12:00:00Z"))
                .build();
    }
}
