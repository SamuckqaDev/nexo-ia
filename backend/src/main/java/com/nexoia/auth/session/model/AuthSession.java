package com.nexoia.auth.session.model;

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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Builder
@Entity
@Table(name = "auth_session")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthSession {

    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SessionStatus status;
    @Column(name = "current_access_jti", nullable = false)
    private UUID currentAccessJti;
    @Column(name = "initial_ip", nullable = false, length = 45)
    private String initialIp;
    @Column(name = "last_ip", nullable = false, length = 45)
    private String lastIp;
    @Column(name = "user_agent", nullable = false, length = 512)
    private String userAgent;
    @Column(name = "access_expires_at", nullable = false)
    private Instant accessExpiresAt;
    @Column(name = "refresh_expires_at", nullable = false)
    private Instant refreshExpiresAt;
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "revoke_reason", length = 120)
    private String revokeReason;

    public boolean isActiveAt(Instant instant) {
        return status == SessionStatus.ACTIVE && refreshExpiresAt.isAfter(instant);
    }

    public void rotateAccess(UUID accessJti, Instant accessExpiry, String ip, Instant seenAt) {
        currentAccessJti = accessJti;
        accessExpiresAt = accessExpiry;
        lastIp = ip;
        lastSeenAt = seenAt;
    }

    public void recordAccess(String ip, Instant seenAt) {
        lastIp = ip;
        lastSeenAt = seenAt;
    }

    public void revoke(SessionStatus newStatus, String reason, Instant revokedAt) {
        status = newStatus;
        revokeReason = reason;
        this.revokedAt = revokedAt;
    }
}
