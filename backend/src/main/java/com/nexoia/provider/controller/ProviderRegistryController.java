package com.nexoia.provider.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.provider.dto.CreateProviderRequest;
import com.nexoia.provider.dto.ProviderConfigurationResponse;
import com.nexoia.provider.service.ProviderRegistryService;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/providers/configurations")
public class ProviderRegistryController {
    private final ProviderRegistryService service;

    @GetMapping
    @Operation(summary = "List the authenticated user's provider configurations")
    public ResponseEntity<BaseResponse<ProviderConfigurationResponse>> list(@AuthenticationPrincipal NexoUserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(200, "Providers retrieved", service.list(principal.userId())));
    }

    @PostMapping
    @Operation(summary = "Save a provider configuration for the authenticated user")
    public ResponseEntity<BaseResponse<ProviderConfigurationResponse>> create(@AuthenticationPrincipal NexoUserPrincipal principal, @Valid @RequestBody CreateProviderRequest request) {
        return ResponseEntity.ok(BaseResponse.success(200, "Provider saved", service.create(principal.userId(), request)));
    }

    @PutMapping("/{providerId}")
    public ResponseEntity<BaseResponse<ProviderConfigurationResponse>> update(@AuthenticationPrincipal NexoUserPrincipal principal, @PathVariable UUID providerId, @Valid @RequestBody CreateProviderRequest request) {
        return ResponseEntity.ok(BaseResponse.success(200, "Provider updated", service.update(principal.userId(), providerId, request)));
    }

    @DeleteMapping("/{providerId}")
    public ResponseEntity<BaseResponse<Void>> remove(@AuthenticationPrincipal NexoUserPrincipal principal, @PathVariable UUID providerId) {
        service.remove(principal.userId(), providerId);
        return ResponseEntity.ok(BaseResponse.success(200, "Provider removed", List.of()));
    }
}
