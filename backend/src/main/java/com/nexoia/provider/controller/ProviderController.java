package com.nexoia.provider.controller;

import com.nexoia.provider.dto.ProviderStatusResponse;
import com.nexoia.provider.service.OllamaProviderService;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/providers")
public class ProviderController {

    private final OllamaProviderService ollamaProviderService;

    @GetMapping("/ollama")
    @Operation(summary = "Inspect the configured local Ollama provider")
    public ResponseEntity<BaseResponse<ProviderStatusResponse>> ollama() {
        return ResponseEntity.ok(BaseResponse.success(200, "Ollama provider inspected",
                ollamaProviderService.status()));
    }
}
