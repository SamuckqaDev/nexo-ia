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
                            .map(model -> {
                                CapabilitySupport support = capabilitySupport(
                                        client, model.name(), model.capabilities());
                                return new ProviderModelResponse(
                                        model.name(), model.modifiedAt(), model.size(),
                                        support.tools(), support.thinking());
                            })
                            .toList();
        } catch (RestClientException exception) {
            throw new ProviderUnavailableException();
        }
    }

    private CapabilitySupport capabilitySupport(
            RestClient client, String model, List<String> listedCapabilities) {
        if (listedCapabilities != null) {
            return supports(listedCapabilities);
        }
        try {
            OllamaShowResponse response = client.post()
                    .uri("/api/show")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OllamaShowRequest(model, false))
                    .retrieve()
                    .body(OllamaShowResponse.class);
            if (response == null || response.capabilities() == null) {
                return CapabilitySupport.unknown();
            }
            return supports(response.capabilities());
        } catch (RestClientException exception) {
            // Older Ollama servers may not expose model capabilities. Catalog discovery remains
            // available, while the frontend presents Agent tool support as unknown.
            return CapabilitySupport.unknown();
        }
    }

    private CapabilitySupport supports(List<String> capabilities) {
        return new CapabilitySupport(
                hasCapability(capabilities, "tools"),
                hasCapability(capabilities, "thinking"));
    }

    private boolean hasCapability(List<String> capabilities, String expected) {
        return capabilities.stream().anyMatch(expected::equalsIgnoreCase);
    }

    private record CapabilitySupport(Boolean tools, Boolean thinking) {

        private static CapabilitySupport unknown() {
            return new CapabilitySupport(null, null);
        }
    }
}
