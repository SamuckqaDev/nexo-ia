package com.nexoia.auth.credential.controller;

import com.nexoia.auth.credential.dto.ChangePasswordRequest;
import com.nexoia.auth.credential.service.PasswordChangeService;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/password")
public class PasswordCredentialController {

    private final PasswordChangeService passwordChangeService;

    @PutMapping
    @Operation(summary = "Change the authenticated user's password and revoke other sessions")
    public ResponseEntity<BaseResponse<Void>> change(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        passwordChangeService.change(principal, request, httpRequest);
        return ResponseEntity.ok(BaseResponse.success(200, "Password changed successfully"));
    }
}
