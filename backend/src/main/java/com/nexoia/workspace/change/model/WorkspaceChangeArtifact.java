package com.nexoia.workspace.change.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A server-owned preview and recovery manifest for one approved workspace file change. */
@Getter
@Builder
@Entity
@Table(name = "workspace_change_artifact")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkspaceChangeArtifact {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "assistant_message_id", nullable = false)
    private UUID assistantMessageId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "workspace_binding_id")
    private UUID workspaceBindingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private WorkspaceChangeOperation operation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkspaceChangeStatus status;

    @Column(name = "relative_path", nullable = false, length = 1024)
    private String relativePath;

    @Column(name = "before_sha256", length = 64)
    private String beforeSha256;

    @Column(name = "after_sha256", length = 64)
    private String afterSha256;

    @Column(name = "before_artifact_key", length = 500)
    private String beforeArtifactKey;

    @Column(name = "after_artifact_key", length = 500)
    private String afterArtifactKey;

    @Column(name = "replacement_count")
    private Integer replacementCount;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "reverted_at")
    private Instant revertedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public void apply(Instant now) {
        status = WorkspaceChangeStatus.APPLIED;
        decidedAt = now;
        appliedAt = now;
        failureCode = null;
    }

    public void deny(Instant now) {
        status = WorkspaceChangeStatus.DENIED;
        decidedAt = now;
    }

    public void invalidate(String code, Instant now) {
        status = WorkspaceChangeStatus.INVALIDATED;
        failureCode = code;
        decidedAt = now;
    }

    public void fail(String code, Instant now) {
        status = WorkspaceChangeStatus.FAILED;
        failureCode = code;
        decidedAt = now;
    }

    public void revert(Instant now) {
        status = WorkspaceChangeStatus.REVERTED;
        revertedAt = now;
        failureCode = null;
    }
}
