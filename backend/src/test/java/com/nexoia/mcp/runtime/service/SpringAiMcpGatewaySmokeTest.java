package com.nexoia.mcp.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.nexoia.mcp.connection.model.McpConnectionKind;
import com.nexoia.mcp.connection.model.McpTransportType;
import com.nexoia.mcp.gateway.service.DockerMcpGatewayRegistry;
import com.nexoia.mcp.runtime.dto.McpRuntimeConnection;
import com.nexoia.mcp.runtime.dto.McpRuntimeTool;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Opt-in smoke test against a real authenticated Docker MCP gateway sidecar. It needs the Compose
 * {@code mcp-duckduckgo} gateway actually running, which the disposable-PostgreSQL {@code docker}
 * suite does not provide. It carries both tags so the standard {@code -Dexcluded.test.groups=ollama,docker}
 * gate skips it, and a clean Testcontainers verify can skip only it with
 * {@code -Dexcluded.test.groups=ollama,mcp-gateway}. Run it deliberately with {@code -Dgroups=mcp-gateway}.
 */
@Tag("docker")
@Tag("mcp-gateway")
class SpringAiMcpGatewaySmokeTest {

    @Test
    void initializesAuthenticatedDockerGatewayAndDiscoversRealTools() {
        String endpoint = System.getenv().getOrDefault(
                "NEXO_SMOKE_MCP_GATEWAY_URL", "http://mcp-duckduckgo:8811/sse");
        String token = System.getenv().getOrDefault(
                "NEXO_SMOKE_MCP_GATEWAY_TOKEN", "nexo-local-mcp-gateway");
        SpringAiMcpClientFactory factory = new SpringAiMcpClientFactory(
                "docker",
                Duration.ofSeconds(30),
                new DockerMcpGatewayRegistry("duckduckgo=" + endpoint, token));
        McpRuntimeConnection connection = new McpRuntimeConnection(
                UUID.randomUUID(),
                "DuckDuckGo",
                McpConnectionKind.DOCKER_CATALOG,
                McpTransportType.DOCKER_GATEWAY,
                "duckduckgo",
                null,
                List.of(new McpRuntimeTool("search", "mcp_test_search")));

        try (McpClientSession session = factory.open(connection)) {
            assertThat(session.snapshot().tools()).extracting(tool -> tool.name())
                    .contains("search", "fetch_content");
            assertThat(session.callbacks()).singleElement().satisfies(callback ->
                    assertThat(callback.getToolDefinition().name()).isEqualTo("mcp_test_search"));
        }
    }

    @Test
    void initializesTheServerMachineProfileWithoutExposingGatewayControlTools() {
        String endpoint = System.getenv("NEXO_SMOKE_MCP_PROFILE_URL");
        assumeTrue(endpoint != null && !endpoint.isBlank());
        String token = System.getenv().getOrDefault(
                "NEXO_SMOKE_MCP_GATEWAY_TOKEN", "nexo-local-mcp-gateway");
        SpringAiMcpClientFactory factory = new SpringAiMcpClientFactory(
                "docker",
                Duration.ofSeconds(60),
                new DockerMcpGatewayRegistry(
                        DockerMcpGatewayRegistry.MACHINE_PROFILE_SERVER_ID + "=" + endpoint,
                        token));
        McpRuntimeConnection connection = new McpRuntimeConnection(
                UUID.randomUUID(),
                "Docker MCP machine profile",
                McpConnectionKind.DOCKER_CATALOG,
                McpTransportType.DOCKER_GATEWAY,
                DockerMcpGatewayRegistry.MACHINE_PROFILE_SERVER_ID,
                null,
                List.of(new McpRuntimeTool("search", "mcp_test_profile_search")));

        try (McpClientSession session = factory.open(connection)) {
            assertThat(session.snapshot().tools()).extracting(tool -> tool.name())
                    .contains("search", "fetch", "sequentialthinking")
                    .doesNotContain("mcp-exec", "mcp-add", "mcp-remove", "code-mode");
            assertThat(session.callbacks()).singleElement().satisfies(callback ->
                    assertThat(callback.getToolDefinition().name())
                            .isEqualTo("mcp_test_profile_search"));
        }
    }
}
