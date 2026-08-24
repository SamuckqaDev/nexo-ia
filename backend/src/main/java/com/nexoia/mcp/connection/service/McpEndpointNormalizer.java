package com.nexoia.mcp.connection.service;

import com.nexoia.mcp.connection.exception.InvalidMcpEndpointException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Validates remote MCP endpoints before the backend dereferences them. */
@Component
public class McpEndpointNormalizer {

    private final boolean allowPrivateEndpoints;

    public McpEndpointNormalizer(
            @Value("${nexo.mcp.allow-private-endpoints:false}") boolean allowPrivateEndpoints) {
        this.allowPrivateEndpoints = allowPrivateEndpoints;
    }

    public String normalize(String value) {
        try {
            URI endpoint = new URI(value.trim()).normalize();
            if (!endpoint.isAbsolute() || endpoint.getHost() == null || endpoint.getUserInfo() != null
                    || endpoint.getRawQuery() != null || endpoint.getRawFragment() != null
                    || !("http".equalsIgnoreCase(endpoint.getScheme())
                    || "https".equalsIgnoreCase(endpoint.getScheme()))) {
                throw new InvalidMcpEndpointException();
            }
            boolean forbidden = Arrays.stream(InetAddress.getAllByName(endpoint.getHost()))
                    .anyMatch(this::forbidden);
            if (forbidden) {
                throw new InvalidMcpEndpointException();
            }
            return endpoint.toString();
        } catch (URISyntaxException | UnknownHostException | IllegalArgumentException exception) {
            throw new InvalidMcpEndpointException();
        }
    }

    private boolean forbidden(InetAddress address) {
        return address.isAnyLocalAddress()
                || ((address.isLoopbackAddress() || address.isSiteLocalAddress())
                && !allowPrivateEndpoints)
                || address.isLinkLocalAddress()
                || address.isMulticastAddress();
    }
}
