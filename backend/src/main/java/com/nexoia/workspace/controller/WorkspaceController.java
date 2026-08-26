package com.nexoia.workspace.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.shared.api.BaseResponse;
import com.nexoia.workspace.dto.BindWorkspaceRequest;
import com.nexoia.workspace.dto.CreateWorkspaceRequest;
import com.nexoia.workspace.dto.WorkspaceFileResponse;
import com.nexoia.workspace.dto.WorkspaceResponse;
import com.nexoia.workspace.dto.WorkspaceStatusResponse;
import com.nexoia.workspace.dto.WorkspaceTreeResponse;
import com.nexoia.workspace.service.WorkspaceService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService service;

    @GetMapping
    @Operation(summary = "List the authenticated user's workspaces")
    public ResponseEntity<BaseResponse<WorkspaceResponse>> list(
            @AuthenticationPrincipal NexoUserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(200, "Workspaces retrieved", service.list(principal.userId())));
    }

    @PostMapping
    @Operation(summary = "Create an unbound workspace owned by the authenticated user")
    public ResponseEntity<BaseResponse<WorkspaceResponse>> create(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @Valid @RequestBody CreateWorkspaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Workspace created", service.create(principal.userId(), request)));
    }

    @GetMapping("/{workspaceId}")
    @Operation(summary = "Get a workspace owned by the authenticated user")
    public ResponseEntity<BaseResponse<WorkspaceResponse>> get(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID workspaceId) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Workspace retrieved", service.get(principal.userId(), workspaceId)));
    }

    @PutMapping("/{workspaceId}/binding")
    @Operation(summary = "Bind a workspace to managed or mounted server storage")
    public ResponseEntity<BaseResponse<WorkspaceResponse>> bind(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody BindWorkspaceRequest request) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Workspace binding updated", service.bind(principal, workspaceId, request)));
    }

    @DeleteMapping("/{workspaceId}")
    @Operation(summary = "Delete a workspace owned by the authenticated user")
    public ResponseEntity<BaseResponse<Void>> delete(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID workspaceId) {
        service.delete(principal.userId(), workspaceId);

        return ResponseEntity.ok(BaseResponse.success(200, "Workspace deleted", List.of()));
    }

    @GetMapping("/{workspaceId}/status")
    @Operation(summary = "Inspect the live availability and structure of a workspace")
    public ResponseEntity<BaseResponse<WorkspaceStatusResponse>> status(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID workspaceId) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Workspace status computed", service.status(principal.userId(), workspaceId)));
    }

    @PostMapping("/{workspaceId}/refresh")
    @Operation(summary = "Rescan a workspace and record its structure fingerprint and Git HEAD")
    public ResponseEntity<BaseResponse<WorkspaceStatusResponse>> refresh(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID workspaceId) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Workspace refreshed", service.refresh(principal.userId(), workspaceId)));
    }

    @GetMapping("/{workspaceId}/tree")
    @Operation(summary = "List one workspace directory lazily, bounded server-side")
    public ResponseEntity<BaseResponse<WorkspaceTreeResponse>> tree(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Workspace tree retrieved", service.tree(principal.userId(), workspaceId, path, limit, cursor)));
    }

    @GetMapping("/{workspaceId}/file")
    @Operation(summary = "Preview a bounded, text-only file inside a workspace")
    public ResponseEntity<BaseResponse<WorkspaceFileResponse>> file(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @RequestParam String path,
            @RequestParam(required = false) Integer startLine,
            @RequestParam(required = false) Integer endLine) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Workspace file retrieved", service.file(principal.userId(), workspaceId, path, startLine, endLine)));
    }
}
