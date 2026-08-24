package com.nexoia.mcp.connection.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class McpToolSelectionException extends ApplicationException {

    public McpToolSelectionException() {
        super(HttpStatus.BAD_REQUEST, "The MCP tool selection contains an unavailable tool");
    }

    public McpToolSelectionException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
