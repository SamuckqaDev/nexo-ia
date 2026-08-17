package com.nexoia.auth.access.controller;

import com.nexoia.auth.access.dto.AccessEventResponse;
import com.nexoia.auth.access.service.AccessMonitoringService;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/access-events")
public class AccessMonitoringController {

    private final AccessMonitoringService accessMonitoringService;

    @GetMapping
    @Operation(summary = "List the current user's recent authentication and access events")
    public ResponseEntity<BaseResponse<AccessEventResponse>> recentEvents(
            @AuthenticationPrincipal NexoUserPrincipal principal) {
        List<AccessEventResponse> events = accessMonitoringService.recentEvents(principal);
        return ResponseEntity.ok(BaseResponse.success(200, "Recent access events retrieved", events));
    }
}
