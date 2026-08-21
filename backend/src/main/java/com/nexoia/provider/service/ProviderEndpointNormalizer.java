package com.nexoia.provider.service;

import com.nexoia.provider.exception.InvalidProviderEndpointException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Normalizes a user-supplied endpoint into an absolute HTTP or HTTPS base URL with no user info,
 * query, or fragment. Shared by saved-configuration persistence and the pre-save connection test so
 * both paths reject the same malformed input the same way.
 *
 * <p>When Nexo IA runs inside a container, a loopback host such as {@code localhost} or
 * {@code 127.0.0.1} points at the container itself, not the machine hosting the user's Ollama. If a
 * container gateway host is configured ({@code nexo.provider.loopback-host}), a loopback endpoint is
 * transparently rewritten to it so the natural value a user types still reaches the host.
 */
@Component
public class ProviderEndpointNormalizer {

    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "0.0.0.0");

    private final String loopbackHost;

    public ProviderEndpointNormalizer(@Value("${nexo.provider.loopback-host:}") String loopbackHost) {
        this.loopbackHost = loopbackHost == null ? "" : loopbackHost.trim();
    }

    public String normalize(String endpoint) {
        try {
            URI uri = new URI(endpoint.trim()).normalize();
            if (!uri.isAbsolute() || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new InvalidProviderEndpointException();
            }

            return rewriteLoopback(uri).toString();
        } catch (URISyntaxException | IllegalArgumentException exception) {
            throw new InvalidProviderEndpointException();
        }
    }

    private URI rewriteLoopback(URI uri) throws URISyntaxException {
        if (loopbackHost.isEmpty() || !LOOPBACK_HOSTS.contains(uri.getHost().toLowerCase())) {
            return uri;
        }

        return new URI(uri.getScheme(), null, loopbackHost, uri.getPort(),
                uri.getPath(), null, null);
    }
}
