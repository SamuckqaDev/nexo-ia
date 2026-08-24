package com.nexoia.mcp.connection.exception;

import com.nexoia.shared.exception.ConflictApplicationException;

public class McpConnectionConflictException extends ConflictApplicationException {

    public McpConnectionConflictException() {
        super("This MCP server is already registered for your account");
    }
}
