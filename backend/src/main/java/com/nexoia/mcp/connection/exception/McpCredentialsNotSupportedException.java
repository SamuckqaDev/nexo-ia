package com.nexoia.mcp.connection.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class McpCredentialsNotSupportedException extends ApplicationException {

    public McpCredentialsNotSupportedException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY,
                "This MCP server requires credentials; encrypted MCP secrets are not available yet");
    }
}
