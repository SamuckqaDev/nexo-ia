package com.nexoia.mcp.gateway.service;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Operator-owned Docker MCP sidecars available to the Nexo backend. */
@Component
public class DockerMcpGatewayRegistry {

    private static final String SERVER_ID_PATTERN = "[a-z0-9][a-z0-9._-]*";

    private final Map<String, URI> endpoints;
    private final String authorizationHeader;

    public DockerMcpGatewayRegistry(
            @Value("${nexo.mcp.docker-gateway-endpoints:}") String configuredEndpoints,
            @Value("${nexo.mcp.docker-gateway-token:}") String gatewayToken) {
        endpoints = parse(configuredEndpoints);
        authorizationHeader = gatewayToken == null || gatewayToken.isBlank()
                ? null
                : "Bearer " + gatewayToken.trim();
    }

    public Optional<URI> endpoint(String serverId) {
        return Optional.ofNullable(endpoints.get(serverId));
    }

    public List<String> serverIds() {
        return List.copyOf(endpoints.keySet());
    }

    public boolean available() {
        return !endpoints.isEmpty();
    }

    public Optional<String> authorizationHeader() {
        return Optional.ofNullable(authorizationHeader);
    }

    private Map<String, URI> parse(String configuredEndpoints) {
        if (configuredEndpoints == null || configuredEndpoints.isBlank()) {
            return Map.of();
        }
        Map<String, URI> parsed = new LinkedHashMap<>();
        Arrays.stream(configuredEndpoints.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(value -> add(parsed, value));
        return Collections.unmodifiableMap(parsed);
    }

    private void add(Map<String, URI> parsed, String value) {
        int separator = value.indexOf('=');
        if (separator <= 0 || separator == value.length() - 1) {
            throw invalidConfiguration();
        }
        String serverId = value.substring(0, separator).trim();
        URI endpoint = URI.create(value.substring(separator + 1).trim());
        if (!serverId.matches(SERVER_ID_PATTERN)
                || endpoint.getHost() == null
                || endpoint.getUserInfo() != null
                || !("http".equalsIgnoreCase(endpoint.getScheme())
                        || "https".equalsIgnoreCase(endpoint.getScheme()))) {
            throw invalidConfiguration();
        }
        if (parsed.putIfAbsent(serverId, endpoint) != null) {
            throw invalidConfiguration();
        }
    }

    private IllegalStateException invalidConfiguration() {
        return new IllegalStateException("Invalid Docker MCP gateway endpoint configuration");
    }
}
