package com.nexoia.mcp.connection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexoia.mcp.connection.exception.InvalidMcpEndpointException;
import org.junit.jupiter.api.Test;

class McpEndpointNormalizerTest {

    @Test
    void blocksLoopbackPrivateAndCredentialBearingEndpointsByDefault() {
        McpEndpointNormalizer normalizer = new McpEndpointNormalizer(false);

        assertThatThrownBy(() -> normalizer.normalize("http://127.0.0.1:8080/mcp"))
                .isInstanceOf(InvalidMcpEndpointException.class);
        assertThatThrownBy(() -> normalizer.normalize("http://user:secret@example.com/mcp"))
                .isInstanceOf(InvalidMcpEndpointException.class);
        assertThatThrownBy(() -> normalizer.normalize("https://example.com/mcp?token=secret"))
                .isInstanceOf(InvalidMcpEndpointException.class);
    }

    @Test
    void permitsLoopbackOnlyWhenTheOperatorExplicitlyAllowsPrivateEndpoints() {
        McpEndpointNormalizer normalizer = new McpEndpointNormalizer(true);

        assertThat(normalizer.normalize("http://127.0.0.1:8080/mcp"))
                .isEqualTo("http://127.0.0.1:8080/mcp");
    }
}
