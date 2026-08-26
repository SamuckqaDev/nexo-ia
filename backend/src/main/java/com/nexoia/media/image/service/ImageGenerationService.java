package com.nexoia.media.image.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditOutcome;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.conversation.chat.exception.ConversationNotFoundException;
import com.nexoia.conversation.chat.repository.ConversationRepository;
import com.nexoia.media.image.config.ComfyUiProperties;
import com.nexoia.media.image.dto.CreateImageGenerationRequest;
import com.nexoia.media.image.dto.ImageContent;
import com.nexoia.media.image.dto.ImageGenerationResponse;
import com.nexoia.media.image.dto.ImageRuntimeResponse;
import com.nexoia.media.image.exception.ImageArtifactPersistenceException;
import com.nexoia.media.image.exception.ImageGenerationNotFoundException;
import com.nexoia.media.image.exception.ImageModelUnavailableException;
import com.nexoia.media.image.exception.ImageRuntimeUnavailableException;
import com.nexoia.media.image.model.ImageGenerationJob;
import com.nexoia.media.image.model.ImageGenerationStatus;
import com.nexoia.media.image.repository.ImageGenerationJobRepository;
import com.nexoia.media.image.runtime.GeneratedImage;
import com.nexoia.media.image.runtime.ImageGenerationRuntime;
import com.nexoia.media.image.runtime.ImageRuntimeHealth;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
public class ImageGenerationService {

    private static final String RUNTIME_FAILURE = "COMFYUI_GENERATION_FAILED";
    private static final String ARTIFACT_FAILURE = "IMAGE_ARTIFACT_PERSISTENCE_FAILED";
    private static final String MODEL_FAILURE = "IMAGE_MODEL_UNAVAILABLE";
    private final ImageGenerationJobRepository jobs;
    private final ConversationRepository conversations;
    private final ImageGenerationRuntime runtime;
    private final ComfyUiProperties properties;
    private final ExecutorService executor;
    private final AuditService audit;
    private final Clock clock;

    public ImageGenerationService(
            ImageGenerationJobRepository jobs,
            ConversationRepository conversations,
            ImageGenerationRuntime runtime,
            ComfyUiProperties properties,
            @Qualifier("imageGenerationExecutor") ExecutorService executor,
            AuditService audit,
            Clock clock) {
        this.jobs = jobs;
        this.conversations = conversations;
        this.runtime = runtime;
        this.properties = properties;
        this.executor = executor;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ImageRuntimeResponse runtime() {
        ImageRuntimeHealth health = runtime.health();
        return new ImageRuntimeResponse(
                runtime.provider(),
                health.configured(),
                health.available(),
                health.model(),
                health.models(),
                health.message());
    }

    @Transactional(readOnly = true)
    public List<ImageGenerationResponse> list(UUID userId, UUID conversationId) {
        ownedConversation(userId, conversationId);
        return jobs.findAllByUserIdAndConversationIdOrderByCreatedAtDesc(userId, conversationId)
                .stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public ImageGenerationResponse create(
            UUID userId,
            UUID conversationId,
            CreateImageGenerationRequest request) {
        ownedConversation(userId, conversationId);
        ImageRuntimeHealth health = runtime.health();
        if (!health.available()) {
            throw new ImageRuntimeUnavailableException();
        }
        String selectedModel = selectedModel(request.model(), health);
        ImageGenerationJob job = jobs.saveAndFlush(ImageGenerationJob.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .conversationId(conversationId)
                .prompt(request.prompt().trim())
                .status(ImageGenerationStatus.QUEUED)
                .provider(runtime.provider())
                .model(selectedModel)
                .build());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                executor.submit(() -> generate(job.getId()));
            }
        });
        audit.record(RecordAuditCommand.success(
                AuditAction.IMAGE_GENERATION_STARTED,
                userId,
                null,
                AuditTargetType.MEDIA,
                job.getId()));
        return response(job);
    }

    @Transactional(readOnly = true)
    public ImageContent content(UUID userId, UUID jobId) {
        ImageGenerationJob job = jobs.findByIdAndUserId(jobId, userId)
                .filter(value -> value.getStatus() == ImageGenerationStatus.COMPLETED)
                .filter(value -> value.getArtifactPath() != null)
                .orElseThrow(ImageGenerationNotFoundException::new);
        Path root = properties.outputDirectory().toAbsolutePath().normalize();
        Path artifact = root.resolve(job.getArtifactPath()).normalize();
        if (!artifact.startsWith(root) || !Files.isRegularFile(artifact)) {
            throw new ImageGenerationNotFoundException();
        }
        try {
            return new ImageContent(
                    Files.readAllBytes(artifact),
                    job.getMediaType() == null ? "image/png" : job.getMediaType(),
                    artifact.getFileName().toString());
        } catch (IOException exception) {
            throw new ImageGenerationNotFoundException();
        }
    }

    private void generate(UUID jobId) {
        try {
            ImageGenerationJob queued = required(jobId);
            GeneratedImage generated = runtime.generate(
                    queued.getPrompt(),
                    queued.getModel(),
                    (runtimeJobId, model) -> markStarted(jobId, runtimeJobId, model));
            String artifact = write(jobId, generated);
            ImageGenerationJob completed = required(jobId);
            completed.complete(artifact, generated.mediaType(), clock.instant());
            jobs.saveAndFlush(completed);
            audit.record(RecordAuditCommand.success(
                    AuditAction.IMAGE_GENERATION_COMPLETED,
                    completed.getUserId(),
                    null,
                    AuditTargetType.MEDIA,
                    completed.getId()));
        } catch (RuntimeException exception) {
            log.warn("[NEXO-BACK][MEDIA] Image job failed jobId={} reason={}",
                    jobId, exception.getClass().getSimpleName());
            String failureCode = switch (exception) {
                case ImageArtifactPersistenceException ignored -> ARTIFACT_FAILURE;
                case ImageModelUnavailableException ignored -> MODEL_FAILURE;
                default -> RUNTIME_FAILURE;
            };
            jobs.findById(jobId).ifPresent(job -> {
                job.fail(failureCode, clock.instant());
                jobs.saveAndFlush(job);
                audit.record(new RecordAuditCommand(
                        AuditAction.IMAGE_GENERATION_FAILED,
                        AuditOutcome.FAILURE,
                        job.getUserId(),
                        null,
                        AuditTargetType.MEDIA,
                        job.getId(),
                        null,
                        failureCode));
            });
        }
    }

    private void markStarted(UUID jobId, String runtimeJobId, String model) {
        ImageGenerationJob job = required(jobId);
        job.start(runtimeJobId, model, clock.instant());
        jobs.saveAndFlush(job);
    }

    private String write(UUID jobId, GeneratedImage generated) {
        String extension = extension(generated.mediaType());
        String filename = jobId + extension;
        Path root = properties.outputDirectory().toAbsolutePath().normalize();
        Path artifact = root.resolve(filename);
        try {
            Files.createDirectories(root);
            Files.write(artifact, generated.bytes());
            return filename;
        } catch (IOException exception) {
            throw new ImageArtifactPersistenceException(exception);
        }
    }

    private String selectedModel(String requestedModel, ImageRuntimeHealth health) {
        if (requestedModel == null || requestedModel.isBlank()) {
            return health.model();
        }
        return health.models().stream()
                .filter(requestedModel::equals)
                .findFirst()
                .orElseThrow(ImageModelUnavailableException::new);
    }

    private String extension(String mediaType) {
        if ("image/jpeg".equalsIgnoreCase(mediaType)) {
            return ".jpg";
        }
        if ("image/webp".equalsIgnoreCase(mediaType)) {
            return ".webp";
        }
        return ".png";
    }

    private ImageGenerationJob required(UUID jobId) {
        return jobs.findById(jobId).orElseThrow(ImageGenerationNotFoundException::new);
    }

    private void ownedConversation(UUID userId, UUID conversationId) {
        conversations.findByIdAndUserIdAndArchivedFalse(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
    }

    private ImageGenerationResponse response(ImageGenerationJob job) {
        return new ImageGenerationResponse(
                job.getId(),
                job.getConversationId(),
                job.getPrompt(),
                job.getStatus(),
                job.getProvider(),
                job.getModel(),
                job.getProgress(),
                job.getEtaSeconds(),
                job.getErrorCode(),
                job.getStatus() == ImageGenerationStatus.COMPLETED
                        ? "/api/v1/media/images/" + job.getId() + "/content"
                        : null,
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt());
    }
}
