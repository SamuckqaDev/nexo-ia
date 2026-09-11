package com.nexoia.provider.service;

import com.nexoia.provider.dto.ProviderModelResponse;
import com.nexoia.provider.exception.ProviderUnavailableException;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.secret.dto.ProviderAuthentication;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Discovers models through vendor APIs without exposing credentials outside the server runtime. */
@Service
public class RemoteProviderModelService {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Autowired
    public RemoteProviderModelService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    public List<ProviderModelResponse> models(
            ProviderType providerType,
            String endpoint,
            ProviderAuthentication authentication) {
        try {
            String body = restClientBuilder.clone().build()
                    .get()
                    .uri(modelsUrl(endpoint))
                    .headers(headers -> authorize(headers, providerType, authentication))
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                data = root.path("models");
            }
            List<ProviderModelResponse> models = new ArrayList<>();
            if (data.isArray()) {
                data.forEach(node -> {
                    String name = text(node, "id", text(node, "name", null));
                    if (name != null && !name.isBlank()) {
                        models.add(new ProviderModelResponse(
                                name,
                                createdAt(node),
                                null,
                                null,
                                null));
                    }
                });
            }
            return List.copyOf(models);
        } catch (RestClientException exception) {
            throw new ProviderUnavailableException();
        }
    }

    private void authorize(
            HttpHeaders headers,
            ProviderType providerType,
            ProviderAuthentication authentication) {
        if (!authentication.configured()) {
            return;
        }
        if (providerType == ProviderType.ANTHROPIC) {
            headers.set("x-api-key", authentication.revealApiKey());
            headers.set("anthropic-version", "2023-06-01");
            return;
        }
        headers.setBearerAuth(authentication.revealApiKey());
    }

    private String modelsUrl(String endpoint) {
        String normalized = endpoint.endsWith("/")
                ? endpoint.substring(0, endpoint.length() - 1)
                : endpoint;
        return normalized + (normalized.matches(".*/v[0-9][^/]*$") || normalized.endsWith("/openai")
                ? "/models"
                : "/v1/models");
    }

    private Instant createdAt(JsonNode node) {
        JsonNode created = node.path("created");
        return created.canConvertToLong() ? Instant.ofEpochSecond(created.asLong()) : null;
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : fallback;
    }
}
