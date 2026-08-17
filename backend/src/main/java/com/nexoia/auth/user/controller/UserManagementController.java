package com.nexoia.auth.user.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.user.dto.CreateMemberRequest;
import com.nexoia.auth.user.dto.ManagedUserResponse;
import com.nexoia.auth.user.dto.UpdateUserStatusRequest;
import com.nexoia.auth.user.service.UserManagementService;
import com.nexoia.auth.session.dto.SessionResponse;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@RequestMapping("/api/v1/admin/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping
    @Operation(summary = "List users managed by the installation Owner")
    public ResponseEntity<BaseResponse<ManagedUserResponse>> list() {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Users retrieved", userManagementService.list()));
    }

    @PostMapping
    @Operation(summary = "Create a Member account")
    public ResponseEntity<BaseResponse<ManagedUserResponse>> createMember(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @Valid @RequestBody CreateMemberRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(BaseResponse.success(200, "Member created",
                userManagementService.createMember(principal, request, httpRequest)));
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Activate or disable a Member account")
    public ResponseEntity<BaseResponse<ManagedUserResponse>> changeStatus(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(BaseResponse.success(200, "User status updated",
                userManagementService.changeStatus(principal, userId, request, httpRequest)));
    }

    @GetMapping("/{userId}/sessions")
    @Operation(summary = "List a Member's active sessions")
    public ResponseEntity<BaseResponse<SessionResponse>> activeSessions(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(BaseResponse.success(200, "Member sessions retrieved",
                userManagementService.activeSessions(userId)));
    }

    @DeleteMapping("/{userId}/sessions/{sessionId}")
    @Operation(summary = "Administratively revoke a Member session")
    public ResponseEntity<BaseResponse<Void>> revokeSession(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID userId,
            @PathVariable UUID sessionId,
            HttpServletRequest httpRequest) {
        userManagementService.revokeSession(principal, userId, sessionId, httpRequest);
        return ResponseEntity.ok(BaseResponse.success(200, "Member session revoked", List.of()));
    }
}
