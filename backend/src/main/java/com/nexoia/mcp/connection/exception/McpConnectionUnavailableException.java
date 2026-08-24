package com.nexoia.mcp.connection.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class McpConnectionUnavailableException extends ApplicationException {

    public McpConnectionUnavailableException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "The MCP server could not be reached");
    }

    public McpConnectionUnavailableException(Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "The MCP server could not be reached", cause);
    }
}
