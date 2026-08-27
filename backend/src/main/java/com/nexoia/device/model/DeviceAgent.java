package com.nexoia.device.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Getter
@Builder
@Entity
@Table(name = "device_agent")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceAgent {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(nullable = false, length = 32)
    private String platform;

    @Column(nullable = false, length = 32)
    private String architecture;

    @Column(name = "app_version", nullable = false, length = 40)
    private String appVersion;

    @Column(name = "credential_hash", nullable = false, unique = true, length = 64)
    private String credentialHash;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private DeviceStatus status = DeviceStatus.OFFLINE;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> capabilities = new ArrayList<>();

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void connected(List<String> availableCapabilities, Instant observedAt) {
        if (status == DeviceStatus.REVOKED) {
            return;
        }
        status = DeviceStatus.ONLINE;
        capabilities = new ArrayList<>(availableCapabilities);
        lastSeenAt = observedAt;
    }

    public void disconnected(Instant observedAt) {
        if (status == DeviceStatus.REVOKED) {
            return;
        }
        status = DeviceStatus.OFFLINE;
        lastSeenAt = observedAt;
    }

    public void heartbeat(Instant observedAt) {
        if (status != DeviceStatus.REVOKED) {
            lastSeenAt = observedAt;
        }
    }

    public void revoke(Instant revokedAt) {
        status = DeviceStatus.REVOKED;
        this.revokedAt = revokedAt;
    }
}
