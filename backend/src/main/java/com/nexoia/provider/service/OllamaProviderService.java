package com.nexoia.provider.service;

import com.nexoia.provider.dto.ProviderModelResponse;
import com.nexoia.provider.dto.ProviderStatusResponse;
import com.nexoia.provider.dto.OllamaTagsResponse;
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
            OllamaTagsResponse response = restClientBuilder.clone().baseUrl(endpoint).build()
                    .get().uri("/api/tags").accept(MediaType.APPLICATION_JSON).retrieve()
                    .body(OllamaTagsResponse.class);
            return response == null || response.models() == null
                    ? List.of()
                    : response.models().stream()
                            .filter(model -> model.name() != null && !model.name().isBlank())
                            .map(model -> new ProviderModelResponse(
                                    model.name(), model.modifiedAt(), model.size()))
                            .toList();
        } catch (RestClientException exception) {
            throw new ProviderUnavailableException();
        }
    }
}
