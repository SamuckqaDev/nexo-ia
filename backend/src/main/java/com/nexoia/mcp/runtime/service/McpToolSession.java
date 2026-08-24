package com.nexoia.mcp.runtime.service;

import com.nexoia.provider.dto.ToolExecutionEvidence;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/** Request-owned MCP clients, governed callbacks, and execution evidence. */
public final class McpToolSession implements AutoCloseable {

    private final List<McpClientSession> clients;
    private final List<ToolCallback> callbacks;
    private final List<ToolExecutionEvidence> evidence;

    McpToolSession(
            List<McpClientSession> clients,
            List<ToolCallback> callbacks,
            List<ToolExecutionEvidence> evidence) {
        this.clients = List.copyOf(clients);
        this.callbacks = List.copyOf(callbacks);
        this.evidence = evidence;
    }

    public List<ToolCallback> callbacks() {
        return callbacks;
    }

    public List<ToolExecutionEvidence> evidence() {
        return List.copyOf(evidence);
    }

    @Override
    public void close() {
        List<McpClientSession> reversed = new ArrayList<>(clients);
        for (McpClientSession client : reversed.reversed()) {
            try {
                client.close();
            } catch (RuntimeException ignored) {
                // The inference result is already determined; closing one transport must not mask it.
            }
        }
    }
}
