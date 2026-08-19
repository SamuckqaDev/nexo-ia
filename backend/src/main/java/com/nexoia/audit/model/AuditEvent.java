package com.nexoia.audit.model;

import com.nexoia.auth.user.model.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One recorded security-relevant action. {@code detail} is a short, safe summary only; it never
 * carries passwords, tokens, message content, or unnecessary personal data.
 */
@Getter
@Builder
@Entity
@Table(name = "audit_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuditEvent {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuditOutcome outcome;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", length = 24)
    private UserRole actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 32)
    private AuditTargetType targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(length = 256)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
