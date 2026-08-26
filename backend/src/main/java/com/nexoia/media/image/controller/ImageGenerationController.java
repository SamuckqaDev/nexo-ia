package com.nexoia.media.image.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.media.image.dto.CreateImageGenerationRequest;
import com.nexoia.media.image.dto.ImageContent;
import com.nexoia.media.image.dto.ImageGenerationResponse;
import com.nexoia.media.image.dto.ImageRuntimeResponse;
import com.nexoia.media.image.service.ImageGenerationService;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/media/images")
public class ImageGenerationController {

    private final ImageGenerationService service;

    @GetMapping("/runtime")
    @Operation(summary = "Inspect the configured local image-generation runtime")
    public ResponseEntity<BaseResponse<ImageRuntimeResponse>> runtime() {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Image runtime inspected", service.runtime()));
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "List image jobs owned by the authenticated conversation owner")
    public ResponseEntity<BaseResponse<ImageGenerationResponse>> list(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID conversationId) {
        return ResponseEntity.ok(BaseResponse.success(
                200,
                "Image generations retrieved",
                service.list(principal.userId(), conversationId)));
    }

    @PostMapping("/conversations/{conversationId}")
    @Operation(summary = "Queue a local ComfyUI image generation for a conversation")
    public ResponseEntity<BaseResponse<ImageGenerationResponse>> create(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody CreateImageGenerationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(BaseResponse.success(
                202,
                "Image generation queued",
                service.create(principal.userId(), conversationId, request)));
    }

    @GetMapping("/{jobId}/content")
    @Operation(summary = "Read a completed image artifact owned by the authenticated user")
    public ResponseEntity<byte[]> content(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID jobId) {
        ImageContent content = service.content(principal.userId(), jobId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(content.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(content.filename()).build().toString())
                .body(content.bytes());
    }
}
