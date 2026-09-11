package com.nexoia.mcp.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexoia.mcp.connection.model.McpConnectionKind;
import com.nexoia.mcp.connection.model.McpTransportType;
import com.nexoia.mcp.gateway.service.DockerMcpGatewayRegistry;
import com.nexoia.mcp.runtime.dto.McpRuntimeConnection;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SpringAiMcpClientFactoryTest {

    @Test
    void usesConfiguredSseSidecarForDockerCatalogServer() {
        SpringAiMcpClientFactory factory = new SpringAiMcpClientFactory(
                "docker",
                Duration.ofSeconds(20),
                new DockerMcpGatewayRegistry("fetch=http://mcp-fetch:8811/sse", "test-token"));

        assertThat(factory.transport(dockerConnection("fetch")))
                .isInstanceOf(HttpClientSseClientTransport.class);
    }

    @Test
    void retainsLocalStdioGatewayWhenNoSidecarIsConfigured() {
        SpringAiMcpClientFactory factory = new SpringAiMcpClientFactory(
                "docker", Duration.ofSeconds(20), new DockerMcpGatewayRegistry("", ""));

        assertThat(factory.transport(dockerConnection("fetch")))
                .isInstanceOf(StdioClientTransport.class);
    }

    @Test
    void hidesMachineProfileControlToolsThatCouldBypassTheUserAllowList() {
        SpringAiMcpClientFactory factory = new SpringAiMcpClientFactory(
                "docker", Duration.ofSeconds(20), new DockerMcpGatewayRegistry("", ""));
        McpRuntimeConnection profile = dockerConnection(
                DockerMcpGatewayRegistry.MACHINE_PROFILE_SERVER_ID);

        assertThat(factory.isExposed(profile, "search")).isTrue();
        assertThat(factory.isExposed(profile, "git_status")).isTrue();
        assertThat(factory.isExposed(profile, "mcp-exec")).isFalse();
        assertThat(factory.isExposed(profile, "code-mode")).isFalse();
    }

    private McpRuntimeConnection dockerConnection(String serverId) {
        return new McpRuntimeConnection(
                UUID.randomUUID(),
                "Docker MCP",
                McpConnectionKind.DOCKER_CATALOG,
                McpTransportType.DOCKER_GATEWAY,
                serverId,
                null,
                List.of());
    }
}
