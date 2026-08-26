package com.nexoia.media.image.model;

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
@Table(name = "image_generation_job")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ImageGenerationJob {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(nullable = false, length = 4000)
    private String prompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ImageGenerationStatus status;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "runtime_job_id", length = 160)
    private String runtimeJobId;

    @Column(length = 255)
    private String model;

    private Integer progress;

    @Column(name = "eta_seconds")
    private Integer etaSeconds;

    @Column(name = "artifact_path", length = 500)
    private String artifactPath;

    @Column(name = "media_type", length = 100)
    private String mediaType;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void start(String runtimeJobId, String model, Instant now) {
        this.status = ImageGenerationStatus.GENERATING;
        this.runtimeJobId = runtimeJobId;
        this.model = model;
        this.startedAt = now;
    }

    public void complete(String artifactPath, String mediaType, Instant now) {
        this.status = ImageGenerationStatus.COMPLETED;
        this.progress = 100;
        this.etaSeconds = 0;
        this.artifactPath = artifactPath;
        this.mediaType = mediaType;
        this.completedAt = now;
        this.errorCode = null;
    }

    public void fail(String errorCode, Instant now) {
        this.status = ImageGenerationStatus.FAILED;
        this.errorCode = errorCode;
        this.completedAt = now;
        this.progress = null;
        this.etaSeconds = null;
    }
}
