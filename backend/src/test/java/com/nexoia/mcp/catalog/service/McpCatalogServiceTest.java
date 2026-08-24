package com.nexoia.mcp.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.nexoia.mcp.connection.model.McpCostType;
import com.nexoia.mcp.gateway.service.DockerMcpGatewayRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.json.JsonMapper;

class McpCatalogServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-24T12:00:00Z");

    @Test
    void readsTheLiveDockerCatalogAndOrdersRecommendedFreeServersFirst() {
        DockerMcpCommandRunner commands = Mockito.mock(DockerMcpCommandRunner.class);
        when(commands.run(List.of("mcp", "version"), Duration.ofSeconds(5)))
                .thenReturn(new DockerMcpCommandResult(0, "v0.43.3"));
        when(commands.run(
                List.of("mcp", "catalog", "server", "ls", "mcp/docker-mcp-catalog", "--format", "json"),
                Duration.ofSeconds(15)))
                .thenReturn(new DockerMcpCommandResult(0, """
                        {"servers":[
                          {"snapshot":{"server":{"name":"paid-api","title":"Paid API","secrets":["TOKEN"]}}},
                          {"snapshot":{"server":{"name":"fetch","title":"Fetch","image":"mcp/fetch",
                            "metadata":{"license":"MIT","category":"web"},"tools":[{"name":"fetch"}]}}}
                        ]}
                        """));

        var result = service(commands, "").catalog();

        assertThat(result.dockerAvailable()).isTrue();
        assertThat(result.gatewayVersion()).isEqualTo("v0.43.3");
        assertThat(result.servers()).extracting(server -> server.id())
                .containsExactly("fetch", "paid-api");
        assertThat(result.servers().getFirst().costType()).isEqualTo(McpCostType.LOCAL_FREE);
        assertThat(result.servers().get(1).requiresSecrets()).isTrue();
    }

    @Test
    void returnsReviewedFreeFallbackWhenDockerMcpIsUnavailable() {
        DockerMcpCommandRunner commands = Mockito.mock(DockerMcpCommandRunner.class);
        when(commands.run(List.of("mcp", "version"), Duration.ofSeconds(5)))
                .thenReturn(new DockerMcpCommandResult(-1, ""));
        when(commands.run(
                List.of("mcp", "catalog", "server", "ls", "mcp/docker-mcp-catalog", "--format", "json"),
                Duration.ofSeconds(15)))
                .thenReturn(new DockerMcpCommandResult(-1, ""));

        var result = service(commands, "").catalog();

        assertThat(result.dockerAvailable()).isFalse();
        assertThat(result.source()).isEqualTo("reviewed-fallback");
        assertThat(result.servers()).extracting(server -> server.id())
                .containsExactly("fetch", "duckduckgo", "git", "playwright");
        assertThat(result.servers()).allMatch(server -> server.costType() == McpCostType.LOCAL_FREE);
    }

    @Test
    void exposesOnlyConfiguredSidecarsWhenTheDockerCliIsUnavailable() {
        DockerMcpCommandRunner commands = Mockito.mock(DockerMcpCommandRunner.class);
        when(commands.run(List.of("mcp", "version"), Duration.ofSeconds(5)))
                .thenReturn(new DockerMcpCommandResult(-1, ""));
        when(commands.run(
                List.of("mcp", "catalog", "server", "ls", "mcp/docker-mcp-catalog", "--format", "json"),
                Duration.ofSeconds(15)))
                .thenReturn(new DockerMcpCommandResult(-1, ""));

        var result = service(commands,
                "fetch=http://mcp-fetch:8811/sse,duckduckgo=http://mcp-duckduckgo:8811/sse")
                .catalog();

        assertThat(result.dockerAvailable()).isTrue();
        assertThat(result.gatewayVersion()).isEqualTo("sidecar");
        assertThat(result.source()).isEqualTo("docker-sidecars");
        assertThat(result.servers()).extracting(server -> server.id())
                .containsExactly("fetch", "duckduckgo");
    }

    private McpCatalogService service(DockerMcpCommandRunner commands, String gateways) {
        return new McpCatalogService(
                commands,
                new DockerMcpGatewayRegistry(gateways, "test-token"),
                JsonMapper.builder().build(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
