package com.nexoia.knowledge.vault.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.knowledge.vault.dto.CreateVaultRequest;
import com.nexoia.knowledge.vault.dto.UpdateVaultRequest;
import com.nexoia.knowledge.vault.dto.VaultResponse;
import com.nexoia.knowledge.vault.service.VaultService;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/knowledge/vaults")
public class VaultController {

    private final VaultService service;

    @GetMapping
    @Operation(summary = "List the authenticated user's active Knowledge Vaults")
    public ResponseEntity<BaseResponse<VaultResponse>> list(
            @AuthenticationPrincipal NexoUserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(200, "Vaults retrieved", service.list(principal.userId())));
    }

    @PostMapping
    @Operation(summary = "Create a Knowledge Vault owned by the authenticated user")
    public ResponseEntity<BaseResponse<VaultResponse>> create(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @Valid @RequestBody CreateVaultRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Vault created", service.create(principal.userId(), request)));
    }

    @PutMapping("/{vaultId}")
    @Operation(summary = "Rename or redescribe a Knowledge Vault owned by the authenticated user")
    public ResponseEntity<BaseResponse<VaultResponse>> update(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID vaultId,
            @Valid @RequestBody UpdateVaultRequest request) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Vault updated", service.update(principal.userId(), vaultId, request)));
    }

    @DeleteMapping("/{vaultId}")
    @Operation(summary = "Archive a Knowledge Vault owned by the authenticated user")
    public ResponseEntity<BaseResponse<Void>> archive(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID vaultId) {
        service.archive(principal.userId(), vaultId);

        return ResponseEntity.ok(BaseResponse.success(200, "Vault archived", List.of()));
    }
}
