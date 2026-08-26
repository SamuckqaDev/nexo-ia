package com.nexoia.knowledge.ingestion.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.knowledge.ingestion.dto.SourceIngestionStatusResponse;
import com.nexoia.knowledge.ingestion.dto.SourceResponse;
import com.nexoia.knowledge.ingestion.service.SourceIngestionService;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class SourceController {

    private final SourceIngestionService service;

    @GetMapping("/vaults/{vaultId}/sources")
    @Operation(summary = "List the sources registered under a Knowledge Vault owned by the authenticated user")
    public ResponseEntity<BaseResponse<SourceResponse>> list(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID vaultId) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Sources retrieved", service.list(principal.userId(), vaultId)));
    }

    @PostMapping(value = "/vaults/{vaultId}/sources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Register and ingest a bounded local source under a Knowledge Vault")
    public ResponseEntity<BaseResponse<SourceResponse>> register(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID vaultId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("displayName") String displayName) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Source registered", service.register(principal.userId(), vaultId, file, displayName)));
    }

    @GetMapping("/sources/{sourceId}/ingestion")
    @Operation(summary = "Read the ingestion status of a registered source")
    public ResponseEntity<BaseResponse<SourceIngestionStatusResponse>> ingestionStatus(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID sourceId) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Ingestion status retrieved",
                service.ingestionStatus(principal.userId(), sourceId)));
    }

    @DeleteMapping("/sources/{sourceId}")
    @Operation(summary = "Archive a source registered under a Knowledge Vault")
    public ResponseEntity<BaseResponse<Void>> archive(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID sourceId) {
        service.archive(principal.userId(), sourceId);

        return ResponseEntity.ok(BaseResponse.success(200, "Source archived", List.of()));
    }
}
