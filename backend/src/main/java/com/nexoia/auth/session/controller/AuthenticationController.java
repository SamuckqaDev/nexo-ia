package com.nexoia.auth.session.controller;

import com.nexoia.auth.session.dto.CsrfResponse;
import com.nexoia.auth.session.dto.LoginRequest;
import com.nexoia.auth.session.service.AuthenticationService;
import com.nexoia.auth.user.dto.UserResponse;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @GetMapping("/csrf")
    @Operation(summary = "Issue a CSRF token for the browser client")
    public ResponseEntity<BaseResponse<CsrfResponse>> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(BaseResponse.success(200, "CSRF token issued",
                new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken())));
    }

    @PostMapping("/login")
    @Operation(summary = "Start a revocable server-side session")
    public ResponseEntity<BaseResponse<UserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return ResponseEntity.ok(BaseResponse.success(200, "Authenticated",
                authenticationService.login(request, httpRequest, httpResponse)));
    }

    @GetMapping("/me")
    @Operation(summary = "Return the current authenticated profile")
    public ResponseEntity<BaseResponse<UserResponse>> me(Authentication authentication) {
        return ResponseEntity.ok(BaseResponse.success(200, "Current profile retrieved",
                authenticationService.currentUser(authentication)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate the refresh token and issue a new access JWT")
    public ResponseEntity<BaseResponse<Void>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        authenticationService.refresh(request, response);
        return ResponseEntity.ok(BaseResponse.success(200, "Tokens refreshed"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current session")
    public ResponseEntity<BaseResponse<Void>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        authenticationService.logout(request, response);
        return ResponseEntity.ok(BaseResponse.success(200, "Session revoked"));
    }
}
