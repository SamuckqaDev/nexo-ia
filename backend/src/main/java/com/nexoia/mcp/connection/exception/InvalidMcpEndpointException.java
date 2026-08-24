package com.nexoia.mcp.connection.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class InvalidMcpEndpointException extends ApplicationException {

    public InvalidMcpEndpointException() {
        super(HttpStatus.BAD_REQUEST, "The MCP endpoint is invalid or not allowed");
    }
}
