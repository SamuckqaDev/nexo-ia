package com.nexoia.provider.service;

import com.nexoia.provider.exception.InvalidProviderEndpointException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Normalizes a user-supplied endpoint into an absolute HTTP or HTTPS base URL with no user info,
 * query, or fragment. Shared by saved-configuration persistence and the pre-save connection test so
 * both paths reject the same malformed input the same way.
 */
final class ProviderEndpointNormalizer {

    private ProviderEndpointNormalizer() {
    }

    static String normalize(String endpoint) {
        try {
            URI uri = new URI(endpoint.trim()).normalize();
            if (!uri.isAbsolute() || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new InvalidProviderEndpointException();
            }
            return uri.toString();
        } catch (URISyntaxException | IllegalArgumentException exception) {
            throw new InvalidProviderEndpointException();
        }
    }
}
