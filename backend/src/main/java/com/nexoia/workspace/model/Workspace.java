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

/**
 * A server-side project the Nexo backend may inspect and (later, behind approval) operate. Every
 * Workspace starts {@code UNBOUND} — a name a Knowledge Vault can target with scope {@code WORKSPACE},
 * with no filesystem access — and becomes readable only after an explicit binding resolves a path
 * inside a configured root. The absolute path is never stored: {@code MANAGED} resolves from
 * {@code ownerId}/{@code id} and {@code MOUNTED} from {@code relativePath}. See D-032.
 */
@Getter
@Builder
@Entity
@Table(name = "workspace")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Workspace {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 160)
    private String name;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 32)
    private WorkspaceStorageType storageType = WorkspaceStorageType.UNBOUND;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "access_mode", nullable = false, length = 40)
    private WorkspaceAccessMode accessMode = WorkspaceAccessMode.READ_ONLY;

    /** Only meaningful for {@code MOUNTED}: the path relative to the configured import root. */
    @Column(name = "relative_path", length = 1024)
    private String relativePath;

    @Column(name = "structure_fingerprint", length = 64)
    private String structureFingerprint;

    @Column(name = "git_head", length = 64)
    private String gitHead;

    @Column(name = "last_scanned_at")
    private Instant lastScannedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isBound() {
        return storageType != WorkspaceStorageType.UNBOUND;
    }

    /**
     * Binds this Workspace to server-side storage. {@code relativePath} is required for {@code MOUNTED}
     * and ignored (nulled) for {@code MANAGED}, whose location derives from owner and id. The previous
     * scan fingerprint is discarded because the target changed.
     */
    public void bind(WorkspaceStorageType storageType, String relativePath, WorkspaceAccessMode accessMode) {
        this.storageType = storageType;
        this.accessMode = accessMode;
        this.relativePath = storageType == WorkspaceStorageType.MOUNTED ? relativePath : null;
        this.structureFingerprint = null;
        this.gitHead = null;
        this.lastScannedAt = null;
    }

    /** Records the outcome of a structure scan so later requests can detect external changes. */
    public void recordScan(String structureFingerprint, String gitHead, Instant scannedAt) {
        this.structureFingerprint = structureFingerprint;
        this.gitHead = gitHead;
        this.lastScannedAt = scannedAt;
    }

    /** Returns the Workspace to an unbound name, preserving its Vault-scope role and dropping access. */
    public void clearBinding() {
        this.storageType = WorkspaceStorageType.UNBOUND;
        this.accessMode = WorkspaceAccessMode.READ_ONLY;
        this.relativePath = null;
        this.structureFingerprint = null;
        this.gitHead = null;
        this.lastScannedAt = null;
    }
}
