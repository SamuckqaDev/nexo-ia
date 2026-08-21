package com.nexoia.knowledge.ingestion.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
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
import com.nexoia.knowledge.retrieval.repository.KnowledgeChunkRepository;
import com.nexoia.knowledge.vault.model.KnowledgeVault;
import com.nexoia.knowledge.vault.service.VaultService;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Registers a source under a Knowledge Vault. Registration is bounded and idempotent by content hash;
 * the normalize/chunk/embed pipeline that turns a REGISTERED source into READY is implemented
 * synchronously, in this same request thread, once {@code EmbeddingClient} exists — no async/task
 * infrastructure exists in the backend to own retry and lifecycle for a background job (D-026).
 */
@Service
@RequiredArgsConstructor
public class SourceIngestionService {

    private static final long MAX_SOURCE_BYTES = 3L * 1024 * 1024;

    /**
     * Extensions this release can extract text from. Everything else is registered and immediately
     * marked {@code UNSUPPORTED} — metadata only, never chunked or embedded. See D-026.
     */
    static final Set<String> INGESTIBLE_EXTENSIONS = Set.of("md", "txt", "json", "csv");

    private final VaultService vaultService;
    private final SourceRepository sources;
    private final KnowledgeChunkRepository chunks;
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
        }

        return response(source);
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
