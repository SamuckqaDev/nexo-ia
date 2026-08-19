package com.nexoia.audit.dto;

import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditOutcome;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.auth.user.model.UserRole;
import java.util.UUID;

/**
 * Internal instruction to record one audit event. It is not an API DTO: callers are services, and
 * {@code detail} must already be a safe summary with no secret or content material.
 */
public record RecordAuditCommand(
        AuditAction action,
        AuditOutcome outcome,
        UUID actorUserId,
        UserRole actorRole,
        AuditTargetType targetType,
        UUID targetId,
        UUID correlationId,
        String detail) {

    public static RecordAuditCommand success(
            AuditAction action, UUID actorUserId, UserRole actorRole,
            AuditTargetType targetType, UUID targetId) {
        return new RecordAuditCommand(
                action, AuditOutcome.SUCCESS, actorUserId, actorRole, targetType, targetId, null, null);
    }
}
