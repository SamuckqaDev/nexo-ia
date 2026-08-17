package com.nexoia.system.controller;

import com.nexoia.shared.api.BaseResponse;
import com.nexoia.system.dto.SystemResponse;
import com.nexoia.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Tag(name = "System", description = "Nexo IA system identity and availability")
public class SystemController {

    private final SystemService systemService;

    @GetMapping
    @Operation(summary = "Get the public system identity")
    public ResponseEntity<BaseResponse<SystemResponse>> getSystemInformation() {
        SystemResponse response = systemService.getSystemInformation();

        return ResponseEntity.ok(BaseResponse.success(
                HttpStatus.OK.value(), "System information retrieved successfully", response));
    }
}
