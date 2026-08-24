package com.nexoia.mcp.runtime.service;

import com.nexoia.mcp.runtime.dto.McpRuntimeConnection;

public interface McpClientFactory {

    McpClientSession open(McpRuntimeConnection connection);
}
