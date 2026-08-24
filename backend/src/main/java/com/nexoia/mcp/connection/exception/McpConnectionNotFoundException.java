package com.nexoia.mcp.connection.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class McpConnectionNotFoundException extends ApplicationException {

    public McpConnectionNotFoundException() {
        super(HttpStatus.NOT_FOUND, "MCP connection not found");
    }
}
