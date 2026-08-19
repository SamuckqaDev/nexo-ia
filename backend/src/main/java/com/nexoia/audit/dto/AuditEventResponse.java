package com.nexoia.audit.dto;

import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditOutcome;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.auth.user.model.UserRole;
import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        AuditAction action,
        AuditOutcome outcome,
        UUID actorUserId,
        UserRole actorRole,
        AuditTargetType targetType,
        UUID targetId,
        UUID correlationId,
        String detail,
        Instant occurredAt) {}
