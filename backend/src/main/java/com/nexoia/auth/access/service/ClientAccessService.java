package com.nexoia.auth.access.service;

import com.nexoia.auth.access.dto.ClientAccessMetadata;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class ClientAccessService {

    private static final int MAX_IP_LENGTH = 45;
    private static final int MAX_USER_AGENT_LENGTH = 512;

    public ClientAccessMetadata extract(HttpServletRequest request) {
        return new ClientAccessMetadata(
                limit(request.getRemoteAddr(), MAX_IP_LENGTH, "unknown"),
                limit(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH, "unknown"));
    }

    private String limit(String value, int maximumLength, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.length() <= maximumLength
                ? normalized
                : normalized.substring(0, maximumLength);
    }
}
