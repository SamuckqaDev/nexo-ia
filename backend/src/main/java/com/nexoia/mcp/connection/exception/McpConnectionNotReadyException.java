package com.nexoia.mcp.connection.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class McpConnectionNotReadyException extends ApplicationException {

    public McpConnectionNotReadyException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY,
                "Inspect the MCP server and enable at least one tool before activating it");
    }
}
