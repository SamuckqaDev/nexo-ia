package com.nexoia.workspace.change.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.shared.api.BaseResponse;
import com.nexoia.workspace.change.dto.WorkspaceChangeResponse;
import com.nexoia.workspace.change.service.WorkspaceChangeService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspace-changes")
public class WorkspaceChangeController {

    private final WorkspaceChangeService service;

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "List server-generated workspace change previews for one conversation")
    public ResponseEntity<BaseResponse<WorkspaceChangeResponse>> list(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID conversationId) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Workspace changes retrieved", service.list(principal.userId(), conversationId)));
    }

    @PostMapping("/{changeId}/approve")
    @Operation(summary = "Apply the exact server-generated workspace preview after revalidation")
    public ResponseEntity<BaseResponse<WorkspaceChangeResponse>> approve(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID changeId) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Workspace change decision recorded", service.approve(principal.userId(), changeId)));
    }

    @PostMapping("/{changeId}/deny")
    @Operation(summary = "Deny a pending workspace change without modifying any file")
    public ResponseEntity<BaseResponse<WorkspaceChangeResponse>> deny(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID changeId) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Workspace change denied", service.deny(principal.userId(), changeId)));
    }

    @PostMapping("/{changeId}/revert")
    @Operation(summary = "Revert an applied workspace change when the file still matches its result")
    public ResponseEntity<BaseResponse<WorkspaceChangeResponse>> revert(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID changeId) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Workspace change revert processed", service.revert(principal.userId(), changeId)));
    }
}
