package com.nexoia.knowledge.ingestion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
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
@Table(name = "knowledge_source")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class KnowledgeSource {

    @Id
    private UUID id;

    @Column(name = "vault_id", nullable = false)
    private UUID vaultId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_kind", nullable = false, length = 16)
    private SourceKind sourceKind;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "byte_size", nullable = false)
    private int byteSize;

    @Column(name = "normalized_content")
    private String normalizedContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SourceStatus status;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(nullable = false)
    private boolean archived;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void markIngesting() {
        this.status = SourceStatus.INGESTING;
        this.errorCode = null;
    }

    public void markReady(Map<String, Object> metadata) {
        this.status = SourceStatus.READY;
        this.metadata = metadata;
        this.errorCode = null;
    }

    public void markUnsupported() {
        this.status = SourceStatus.UNSUPPORTED;
        this.normalizedContent = null;
    }

    public void markFailed(String errorCode) {
        this.status = SourceStatus.FAILED;
        this.errorCode = errorCode;
    }

    public void archive() {
        this.archived = true;
    }
}
