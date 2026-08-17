package com.nexoia.auth.session.controller;

import com.nexoia.auth.session.dto.SessionResponse;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.session.service.SessionManagementService;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/sessions")
public class SessionController {

    private final SessionManagementService sessionManagementService;

    @GetMapping
    @Operation(summary = "List the current user's active access sessions")
    public ResponseEntity<BaseResponse<SessionResponse>> activeSessions(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            HttpServletRequest request) {
        List<SessionResponse> sessions = sessionManagementService.activeSessions(principal, request);
        return ResponseEntity.ok(BaseResponse.success(200, "Active sessions retrieved", sessions));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Revoke one of the current user's other active sessions")
    public ResponseEntity<BaseResponse<Void>> revoke(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID sessionId,
            HttpServletRequest request) {
        sessionManagementService.revoke(principal, sessionId, request);
        return ResponseEntity.ok(BaseResponse.success(200, "Session revoked", List.of()));
    }

    @PostMapping("/revoke-others")
    @Operation(summary = "Revoke every active session except the current one")
    public ResponseEntity<BaseResponse<Void>> revokeOthers(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            HttpServletRequest request) {
        sessionManagementService.revokeOthers(principal, request);
        return ResponseEntity.ok(BaseResponse.success(
                200, "Other sessions revoked", List.of()));
    }
}
