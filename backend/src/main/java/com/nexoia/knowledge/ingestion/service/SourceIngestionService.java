package com.nexoia.knowledge.ingestion.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditOutcome;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.knowledge.ingestion.dto.SourceIngestionStatusResponse;
import com.nexoia.knowledge.ingestion.dto.SourceResponse;
import com.nexoia.knowledge.ingestion.exception.SourceNotFoundException;
import com.nexoia.knowledge.ingestion.exception.SourceTooLargeException;
import com.nexoia.knowledge.ingestion.exception.UnsupportedSourceTypeException;
import com.nexoia.knowledge.ingestion.model.KnowledgeSource;
import com.nexoia.knowledge.ingestion.model.SourceKind;
import com.nexoia.knowledge.ingestion.model.SourceStatus;
import com.nexoia.knowledge.ingestion.repository.SourceRepository;
import com.nexoia.knowledge.embedding.dto.EmbeddingOutcome;
import com.nexoia.knowledge.embedding.exception.EmbeddingProviderUnavailableException;
import com.nexoia.knowledge.embedding.service.EmbeddingService;
import com.nexoia.knowledge.retrieval.model.KnowledgeChunk;
import com.nexoia.knowledge.retrieval.repository.KnowledgeChunkRepository;
import com.nexoia.knowledge.vault.model.KnowledgeVault;
import com.nexoia.knowledge.vault.service.VaultService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;

/**
 * Registers a source under a Knowledge Vault and runs its normalize/chunk/embed pipeline synchronously,
 * in this same request thread — no async/task infrastructure exists in the backend to own retry and
 * lifecycle for a background job (D-026).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SourceIngestionService {

    private static final long MAX_SOURCE_BYTES = 3L * 1024 * 1024;
    private static final String ERROR_EXTRACTION_FAILED = "INGESTION_EXTRACTION_FAILED";
    private static final String ERROR_EMBEDDING_UNAVAILABLE = "INGESTION_EMBEDDING_UNAVAILABLE";
    private static final String ERROR_UNEXPECTED = "INGESTION_FAILED";

    /**
     * Extensions this release can extract text from. Everything else is registered and immediately
     * marked {@code UNSUPPORTED} — metadata only, never chunked or embedded. See D-026.
     */
    static final Set<String> INGESTIBLE_EXTENSIONS = Set.of("md", "txt", "json", "csv");

    private final VaultService vaultService;
    private final SourceRepository sources;
    private final KnowledgeChunkRepository chunks;
    private final SourceNormalizer normalizer;
    private final ChunkingService chunking;
    private final EmbeddingService embeddingService;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<SourceResponse> list(UUID ownerId, UUID vaultId) {
        vaultService.ownedVault(ownerId, vaultId);

        return sources.findAllByVaultIdAndArchivedFalseOrderByCreatedAtDesc(vaultId).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public SourceResponse register(UUID ownerId, UUID vaultId, MultipartFile file, String displayName) {
        KnowledgeVault vault = vaultService.ownedVault(ownerId, vaultId);

        if (file.isEmpty() || displayName == null || displayName.isBlank()) {
            throw new UnsupportedSourceTypeException();
        }
        if (file.getSize() > MAX_SOURCE_BYTES) {
            throw new SourceTooLargeException();
        }

        byte[] content = readBytes(file);
        String extension = extensionOf(displayName.trim());
        String contentHash = sha256(content);

        return sources.findByVaultIdAndContentHash(vaultId, contentHash)
                .map(this::response)
                .orElseGet(() -> registerNew(vault, displayName.trim(), extension, content, contentHash));
    }

    @Transactional(readOnly = true)
    public SourceIngestionStatusResponse ingestionStatus(UUID ownerId, UUID sourceId) {
        KnowledgeSource source = ownedSource(ownerId, sourceId);
        int chunkCount = chunks.findAllBySourceIdOrderByOrdinalAsc(source.getId()).size();

        return new SourceIngestionStatusResponse(
                source.getStatus(), source.getErrorCode(), chunkCount,
                source.getContentHash(), source.getByteSize(), source.getMimeType());
    }

    @Transactional
    public void archive(UUID ownerId, UUID sourceId) {
        ownedSource(ownerId, sourceId).archive();
        audit.record(RecordAuditCommand.success(
                AuditAction.SOURCE_ARCHIVED, ownerId, null, AuditTargetType.KNOWLEDGE_SOURCE, sourceId));
    }

    private SourceResponse registerNew(
            KnowledgeVault vault, String displayName, String extension, byte[] content, String contentHash) {
        KnowledgeSource source = sources.save(KnowledgeSource.builder()
                .id(UUID.randomUUID())
                .vaultId(vault.getId())
                .sourceKind(SourceKind.UPLOAD)
                .displayName(displayName)
                .mimeType(mimeTypeFor(extension))
                .contentHash(contentHash)
                .byteSize(content.length)
                .status(SourceStatus.REGISTERED)
                .archived(false)
                .build());
        audit.record(RecordAuditCommand.success(
                AuditAction.SOURCE_REGISTERED, vault.getOwnerId(), null,
                AuditTargetType.KNOWLEDGE_SOURCE, source.getId()));

        if (!INGESTIBLE_EXTENSIONS.contains(extension)) {
            source.markUnsupported();
            return response(source);
        }

        ingest(vault, source, extension, content);

        return response(source);
    }

    private void ingest(KnowledgeVault vault, KnowledgeSource source, String extension, byte[] content) {
        source.markIngesting();
        audit.record(RecordAuditCommand.success(
                AuditAction.SOURCE_INGESTION_STARTED, vault.getOwnerId(), null,
                AuditTargetType.KNOWLEDGE_SOURCE, source.getId()));

        try {
            String normalizedText = normalizer.normalize(extension, content);
            List<ChunkingService.ChunkDraft> drafts = chunking.chunk(normalizedText);

            if (drafts.isEmpty()) {
                source.markReady(Map.of("chunkCount", 0));
            } else {
                EmbeddingOutcome outcome = embeddingService.embed(
                        vault.getOwnerId(), drafts.stream().map(ChunkingService.ChunkDraft::content).toList());
                chunks.saveAll(chunkEntities(source, drafts, outcome));
                source.markReady(Map.of("chunkCount", drafts.size()));
            }
            audit.record(RecordAuditCommand.success(
                    AuditAction.SOURCE_INGESTION_COMPLETED, vault.getOwnerId(), null,
                    AuditTargetType.KNOWLEDGE_SOURCE, source.getId()));
        } catch (EmbeddingProviderUnavailableException exception) {
            failIngestion(vault, source, ERROR_EMBEDDING_UNAVAILABLE, exception);
        } catch (RuntimeException exception) {
            failIngestion(vault, source, isExtractionFailure(exception) ? ERROR_EXTRACTION_FAILED : ERROR_UNEXPECTED,
                    exception);
        }
    }

    private void failIngestion(KnowledgeVault vault, KnowledgeSource source, String errorCode, RuntimeException cause) {
        UUID correlationId = UUID.randomUUID();
        log.warn("[NEXO-BACK][KNOWLEDGE] Ingestion failed sourceId={} correlationId={} errorCode={} reason={}",
                source.getId(), correlationId, errorCode, cause.getClass().getSimpleName());
        source.markFailed(errorCode);
        audit.record(new RecordAuditCommand(
                AuditAction.SOURCE_INGESTION_FAILED, AuditOutcome.FAILURE,
                vault.getOwnerId(), null, AuditTargetType.KNOWLEDGE_SOURCE, source.getId(),
                correlationId, errorCode));
    }

    private boolean isExtractionFailure(RuntimeException exception) {
        return exception instanceof UncheckedIOException || exception instanceof JacksonException;
    }

    private List<KnowledgeChunk> chunkEntities(
            KnowledgeSource source, List<ChunkingService.ChunkDraft> drafts, EmbeddingOutcome outcome) {
        List<KnowledgeChunk> entities = new ArrayList<>(drafts.size());
        for (int index = 0; index < drafts.size(); index++) {
            ChunkingService.ChunkDraft draft = drafts.get(index);
            entities.add(KnowledgeChunk.builder()
                    .id(UUID.randomUUID())
                    .sourceId(source.getId())
                    .ordinal(draft.ordinal())
                    .content(draft.content())
                    .tokenEstimate(draft.tokenEstimate())
                    .embedding(outcome.embeddings().get(index))
                    .embeddingModel(outcome.model())
                    .embeddingDimensions(outcome.dimensions())
                    .build());
        }

        return entities;
    }

    /**
     * Resolves a source by id alone (the archive and ingestion-status endpoints are not nested under
     * a vault path), then confirms the caller owns its vault — PILL-007 style: a source under another
     * owner's vault answers 404, not 403.
     */
    private KnowledgeSource ownedSource(UUID ownerId, UUID sourceId) {
        KnowledgeSource source = sources.findByIdAndArchivedFalse(sourceId)
                .orElseThrow(SourceNotFoundException::new);
        vaultService.ownedVault(ownerId, source.getVaultId());

        return source;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new UnsupportedSourceTypeException();
        }
    }

    private String extensionOf(String displayName) {
        int dot = displayName.lastIndexOf('.');
        return dot < 0 || dot == displayName.length() - 1
                ? ""
                : displayName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String mimeTypeFor(String extension) {
        return switch (extension) {
            case "md" -> "text/markdown";
            case "txt" -> "text/plain";
            case "json" -> "application/json";
            case "csv" -> "text/csv";
            default -> "application/octet-stream";
        };
    }

    private String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private SourceResponse response(KnowledgeSource value) {
        return new SourceResponse(
                value.getId(),
                value.getVaultId(),
                value.getSourceKind(),
                value.getDisplayName(),
                value.getMimeType(),
                value.getByteSize(),
                value.getStatus(),
                value.getErrorCode(),
                value.getCreatedAt(),
                value.getUpdatedAt());
    }
}
