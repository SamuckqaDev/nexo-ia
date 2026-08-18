package com.nexoia.usage.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.shared.api.BaseResponse;
import com.nexoia.usage.dto.UsageSummaryResponse;
import com.nexoia.usage.model.UsagePeriod;
import com.nexoia.usage.service.UsageService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/usage")
public class UsageController {

    private final UsageService service;

    @GetMapping
    @Operation(summary = "Report the authenticated member's own model usage")
    public ResponseEntity<BaseResponse<UsageSummaryResponse>> summary(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @RequestParam(defaultValue = "LAST_7_DAYS") UsagePeriod period) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Usage retrieved", service.summary(principal.userId(), period)));
    }
}
