package com.nexoia.auth.profile.controller;

import com.nexoia.auth.profile.dto.UpdateProfileRequest;
import com.nexoia.auth.profile.service.ProfileService;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.user.dto.UserResponse;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/auth/profile")
public class ProfileController {

    private final ProfileService profileService;

    @PutMapping
    @Operation(summary = "Update the current authenticated profile")
    public ResponseEntity<BaseResponse<UserResponse>> update(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(BaseResponse.success(200, "Profile updated",
                profileService.update(principal.userId(), request)));
    }
}
