package com.nexoia.workspace.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.shared.api.BaseResponse;
import com.nexoia.workspace.dto.CreateWorkspaceRequest;
import com.nexoia.workspace.dto.WorkspaceResponse;
import com.nexoia.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    @Operation(summary = "Create a workspace owned by the authenticated user")
    public ResponseEntity<BaseResponse<WorkspaceResponse>> create(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @Valid @RequestBody CreateWorkspaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Workspace created", service.create(principal.userId(), request)));
    }
}
