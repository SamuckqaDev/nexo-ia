package com.nexoia.provider.service;

import com.nexoia.provider.dto.OllamaShowRequest;
import com.nexoia.provider.dto.OllamaShowResponse;
import com.nexoia.provider.dto.OllamaTagsResponse;
import com.nexoia.provider.dto.ProviderModelResponse;
import com.nexoia.provider.dto.ProviderStatusResponse;
import com.nexoia.provider.exception.ProviderUnavailableException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class OllamaProviderService {

    private final RestClient.Builder restClientBuilder;

    @Value("${spring.ai.ollama.base-url}")
    private String baseUrl;

    public ProviderStatusResponse status() {
        List<ProviderModelResponse> models = models(baseUrl);
        return new ProviderStatusResponse("ollama", "Ollama", "LOCAL", baseUrl,
                true, models);
    }

    public List<ProviderModelResponse> models(String endpoint) {
        try {
            RestClient client = restClientBuilder.clone().baseUrl(endpoint).build();
            OllamaTagsResponse response = client
                    .get().uri("/api/tags").accept(MediaType.APPLICATION_JSON).retrieve()
                    .body(OllamaTagsResponse.class);
            return response == null || response.models() == null
                    ? List.of()
                    : response.models().stream()
                            .filter(model -> model.name() != null && !model.name().isBlank())
                            .map(model -> new ProviderModelResponse(
                                    model.name(), model.modifiedAt(), model.size(),
                                    toolCallingSupport(client, model.name(), model.capabilities())))
                            .toList();
        } catch (RestClientException exception) {
            throw new ProviderUnavailableException();
        }
    }

    private Boolean toolCallingSupport(
            RestClient client, String model, List<String> listedCapabilities) {
        if (listedCapabilities != null) {
            return supportsTools(listedCapabilities);
        }
        try {
            OllamaShowResponse response = client.post()
                    .uri("/api/show")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OllamaShowRequest(model, false))
                    .retrieve()
                    .body(OllamaShowResponse.class);
            if (response == null || response.capabilities() == null) {
                return null;
            }
            return supportsTools(response.capabilities());
        } catch (RestClientException exception) {
            // Older Ollama servers may not expose model capabilities. Catalog discovery remains
            // available, while the frontend presents Agent tool support as unknown.
            return null;
        }
    }

    private boolean supportsTools(List<String> capabilities) {
        return capabilities.stream()
                .anyMatch(capability -> "tools".equalsIgnoreCase(capability));
    }
}
