package com.nexoia.auth.recovery.controller;

import com.nexoia.auth.access.dto.ClientAccessMetadata;
import com.nexoia.auth.access.service.ClientAccessService;
import com.nexoia.auth.recovery.dto.PasswordResetConfirmation;
import com.nexoia.auth.recovery.dto.PasswordResetRequest;
import com.nexoia.auth.recovery.service.PasswordRecoveryService;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/password")
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;
    private final ClientAccessService clientAccessService;

    @PostMapping("/forgot")
    @Operation(summary = "Request a password reset without revealing account existence")
    public ResponseEntity<BaseResponse<Void>> forgot(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {
        ClientAccessMetadata metadata = clientAccessService.extract(httpRequest);
        passwordRecoveryService.request(request.email(), metadata.ipAddress());
        return ResponseEntity.ok(BaseResponse.success(200,
                "If the account exists, password reset instructions were sent"));
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset a password using a single-use recovery token")
    public ResponseEntity<BaseResponse<Void>> reset(
            @Valid @RequestBody PasswordResetConfirmation request) {
        passwordRecoveryService.reset(request);
        return ResponseEntity.ok(BaseResponse.success(200, "Password reset successfully"));
    }
}
