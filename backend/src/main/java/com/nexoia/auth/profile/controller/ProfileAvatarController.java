package com.nexoia.auth.profile.controller;

import com.nexoia.auth.profile.dto.ProfileAvatarContent;
import com.nexoia.auth.profile.service.ProfileAvatarService;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/profile/avatar")
public class ProfileAvatarController {
    private final ProfileAvatarService service;

    @GetMapping
    @Operation(summary = "Return the authenticated user's profile avatar")
    public ResponseEntity<byte[]> get(@AuthenticationPrincipal NexoUserPrincipal principal) {
        ProfileAvatarContent avatar = service.get(principal.userId());
        return ResponseEntity.ok().cacheControl(CacheControl.noCache())
                .contentType(MediaType.parseMediaType(avatar.contentType())).body(avatar.content());
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Replace the authenticated user's profile avatar")
    public ResponseEntity<BaseResponse<Void>> save(@AuthenticationPrincipal NexoUserPrincipal principal,
            @RequestPart("avatar") MultipartFile avatar) {
        service.save(principal.userId(), avatar);
        return ResponseEntity.ok(BaseResponse.success(200, "Profile avatar updated", List.of()));
    }

    @DeleteMapping
    @Operation(summary = "Remove the authenticated user's profile avatar")
    public ResponseEntity<BaseResponse<Void>> delete(@AuthenticationPrincipal NexoUserPrincipal principal) {
        service.delete(principal.userId());
        return ResponseEntity.ok(BaseResponse.success(200, "Profile avatar removed", List.of()));
    }
}
