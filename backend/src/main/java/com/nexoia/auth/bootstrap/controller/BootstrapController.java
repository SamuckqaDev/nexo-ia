package com.nexoia.auth.bootstrap.controller;

import com.nexoia.auth.bootstrap.dto.BootstrapStatusResponse;
import com.nexoia.auth.bootstrap.dto.CreateOwnerRequest;
import com.nexoia.auth.bootstrap.service.BootstrapService;
import com.nexoia.auth.user.dto.UserResponse;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/bootstrap")
public class BootstrapController {

    private final BootstrapService bootstrapService;

    @GetMapping
    @Operation(summary = "Check whether the first Owner must be created")
    public ResponseEntity<BaseResponse<BootstrapStatusResponse>> status() {
        return ResponseEntity.ok(BaseResponse.success(200, "Bootstrap status retrieved",
                bootstrapService.status()));
    }

    @PostMapping
    @Operation(summary = "Create the first Nexo Owner")
    public ResponseEntity<BaseResponse<UserResponse>> createOwner(
            @Valid @RequestBody CreateOwnerRequest request) {
        return ResponseEntity.status(201).body(BaseResponse.success(201, "Owner created",
                bootstrapService.createOwner(request)));
    }
}
