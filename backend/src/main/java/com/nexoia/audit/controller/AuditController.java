package com.nexoia.audit.controller;

import com.nexoia.audit.dto.AuditEventResponse;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.service.AuditService;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The audit trail is administrative. Only an Owner may inspect it, so a member can never read another
 * member's recorded activity.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
@RequestMapping("/api/v1/admin/audit")
public class AuditController {

    private final AuditService service;

    @GetMapping
    @Operation(summary = "Inspect the security audit trail")
    public ResponseEntity<BaseResponse<AuditEventResponse>> list(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(BaseResponse.success(200, "Audit trail retrieved", service.query(
                Optional.ofNullable(action),
                Optional.ofNullable(actorUserId),
                limit == null ? service.defaultLimit() : limit)));
    }
}
