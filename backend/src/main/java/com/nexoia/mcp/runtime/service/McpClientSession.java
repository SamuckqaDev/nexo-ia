package com.nexoia.mcp.runtime.service;

import com.nexoia.mcp.runtime.dto.McpConnectionSnapshot;
import io.modelcontextprotocol.client.McpSyncClient;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

public record McpClientSession(
        McpSyncClient client,
        McpConnectionSnapshot snapshot,
        List<ToolCallback> callbacks) implements AutoCloseable {

    @Override
    public void close() {
        client.closeGracefully();
    }
}
