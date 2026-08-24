package com.nexoia.mcp.connection.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class McpConfigurationNotSupportedException extends ApplicationException {

    public McpConfigurationNotSupportedException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY,
                "This MCP server needs configuration; isolated per-user MCP configuration is not available yet");
    }
}
