package com.nexoia.mcp.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DockerMcpGatewayRegistryTest {

    @Test
    void parsesOperatorOwnedGatewayEndpointsInOrder() {
        DockerMcpGatewayRegistry registry = new DockerMcpGatewayRegistry(
                "fetch=http://mcp-fetch:8811/sse,duckduckgo=http://mcp-duckduckgo:8811/sse",
                "test-token");

        assertThat(registry.available()).isTrue();
        assertThat(registry.serverIds()).containsExactly("fetch", "duckduckgo");
        assertThat(registry.endpoint("fetch")).hasValueSatisfying(endpoint ->
                assertThat(endpoint.toString()).isEqualTo("http://mcp-fetch:8811/sse"));
        assertThat(registry.authorizationHeader()).contains("Bearer test-token");
    }

    @Test
    void rejectsCredentialsAndDuplicateServerIds() {
        assertThatThrownBy(() -> new DockerMcpGatewayRegistry(
                "fetch=http://user:secret@mcp-fetch:8811/mcp", "test-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Invalid Docker MCP gateway endpoint configuration");
        assertThatThrownBy(() -> new DockerMcpGatewayRegistry(
                "fetch=http://one:8811/mcp,fetch=http://two:8811/mcp", "test-token"))
                .isInstanceOf(IllegalStateException.class);
    }
}
