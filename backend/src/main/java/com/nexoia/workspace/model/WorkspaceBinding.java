package com.nexoia.workspace.model;

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
@Table(name = "workspace_binding")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkspaceBinding {

    @Id
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "local_binding_id", nullable = false, length = 120)
    private String localBindingId;

    @Column(name = "display_name", nullable = false, length = 240)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private WorkspaceBindingStatus status;

    @Column(name = "structure_fingerprint", length = 64)
    private String structureFingerprint;

    @Column(name = "git_head", length = 64)
    private String gitHead;

    @Column(name = "git_branch", length = 240)
    private String gitBranch;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void refresh(
            String displayName,
            WorkspaceBindingStatus status,
            String structureFingerprint,
            String gitHead,
            String gitBranch,
            Instant observedAt) {
        this.displayName = displayName;
        this.status = status;
        this.structureFingerprint = structureFingerprint;
        this.gitHead = gitHead;
        this.gitBranch = gitBranch;
        this.lastSeenAt = observedAt;
    }
}
