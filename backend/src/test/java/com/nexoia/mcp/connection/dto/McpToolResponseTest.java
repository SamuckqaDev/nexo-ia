package com.nexoia.mcp.connection.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class McpToolResponseTest {

    @Test
    void serializesTheToolNameUsingThePublicExternalNameContract() {
        McpToolResponse response = new McpToolResponse(
                "fetch", "mcp_12345678_fetch", "Fetch", "Fetch a public page",
                false, true, false, false, Instant.parse("2026-08-24T12:00:00Z"));

        String json = JsonMapper.builder().findAndAddModules().build().writeValueAsString(response);

        assertThat(json)
                .contains("\"externalName\":\"fetch\"")
                .doesNotContain("\"name\":\"fetch\"");
    }
}
