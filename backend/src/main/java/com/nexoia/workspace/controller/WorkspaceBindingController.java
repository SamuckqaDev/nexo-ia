package com.nexoia.workspace.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.shared.api.BaseResponse;
import com.nexoia.workspace.dto.WorkspaceBindingResponse;
import com.nexoia.workspace.dto.WorkspaceTreeResponse;
import com.nexoia.workspace.service.LocalWorkspaceBrowserService;
import com.nexoia.workspace.service.WorkspaceBindingService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceId}/bindings")
public class WorkspaceBindingController {

    private final WorkspaceBindingService bindings;
    private final LocalWorkspaceBrowserService browser;

    @GetMapping
    @Operation(summary = "List the authenticated user's device bindings for one workspace")
    public ResponseEntity<BaseResponse<WorkspaceBindingResponse>> list(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID workspaceId) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Workspace bindings retrieved", bindings.list(principal.userId(), workspaceId)));
    }

    @GetMapping("/{bindingId}/tree")
    @Operation(summary = "List one directory through an online Nexo Desktop binding")
    public ResponseEntity<BaseResponse<WorkspaceTreeResponse>> tree(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID workspaceId,
            @PathVariable UUID bindingId,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Local workspace tree retrieved",
                browser.tree(principal.userId(), workspaceId, bindingId, path, limit, cursor)));
    }
}
