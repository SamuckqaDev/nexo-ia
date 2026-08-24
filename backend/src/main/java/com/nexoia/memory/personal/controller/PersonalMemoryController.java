package com.nexoia.memory.personal.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.memory.personal.dto.CreatePersonalMemoryRequest;
import com.nexoia.memory.personal.dto.PersonalMemoryResponse;
import com.nexoia.memory.personal.service.PersonalMemoryService;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/memories")
public class PersonalMemoryController {

    private final PersonalMemoryService service;

    @GetMapping
    @Operation(summary = "List personal memories owned by the authenticated user")
    public ResponseEntity<BaseResponse<PersonalMemoryResponse>> list(
            @AuthenticationPrincipal NexoUserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Personal memories retrieved", service.list(principal.userId())));
    }

    @PostMapping
    @Operation(summary = "Create a personal memory for the authenticated user")
    public ResponseEntity<BaseResponse<PersonalMemoryResponse>> create(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @Valid @RequestBody CreatePersonalMemoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Personal memory created",
                service.remember(principal.userId(), request.content(), null, null)));
    }

    @DeleteMapping("/{memoryId}")
    @Operation(summary = "Remove a personal memory owned by the authenticated user")
    public ResponseEntity<BaseResponse<Void>> remove(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID memoryId) {
        service.remove(principal.userId(), memoryId);
        return ResponseEntity.ok(BaseResponse.success(200, "Personal memory removed", List.of()));
    }
}
