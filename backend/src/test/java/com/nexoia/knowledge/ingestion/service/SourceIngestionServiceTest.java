package com.nexoia.knowledge.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.knowledge.embedding.service.EmbeddingService;
import com.nexoia.knowledge.ingestion.dto.SourceResponse;
import com.nexoia.knowledge.ingestion.model.KnowledgeSource;
import com.nexoia.knowledge.ingestion.model.SourceKind;
import com.nexoia.knowledge.ingestion.model.SourceStatus;
import com.nexoia.knowledge.ingestion.repository.SourceRepository;
import com.nexoia.knowledge.retrieval.repository.KnowledgeChunkRepository;
import com.nexoia.knowledge.vault.model.KnowledgeVault;
import com.nexoia.knowledge.vault.model.VaultOwnerType;
import com.nexoia.knowledge.vault.model.VaultScope;
import com.nexoia.knowledge.vault.service.VaultService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class SourceIngestionServiceTest {

    @Mock private VaultService vaultService;
    @Mock private SourceRepository sources;
    @Mock private KnowledgeChunkRepository chunks;
    @Mock private SourceNormalizer normalizer;
    @Mock private ChunkingService chunking;
    @Mock private EmbeddingService embeddingService;
    @Mock private AuditService audit;
    @Mock private MultipartFile file;

    private SourceIngestionService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID vaultId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SourceIngestionService(
                vaultService, sources, chunks, normalizer, chunking, embeddingService, audit);
        when(sources.saveAndFlush(any(KnowledgeSource.class)))
                .thenAnswer(invocation -> persisted(invocation.getArgument(0)));
        when(sources.findByVaultIdAndContentHash(any(), anyString())).thenReturn(Optional.empty());
        when(normalizer.normalize(anyString(), any())).thenReturn("Nexo knowledge");
        when(chunking.chunk(anyString())).thenReturn(List.of());
    }

    @Test
    void flushesAnUploadedSourceBeforeReturningItsTimestampedResponse() throws Exception {
        when(vaultService.manageableVault(userId, vaultId)).thenReturn(vault(false));
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(14L);
        when(file.getBytes()).thenReturn("Nexo knowledge".getBytes(StandardCharsets.UTF_8));

        SourceResponse response = service.register(userId, vaultId, file, "notes.md");

        assertThat(response.sourceKind()).isEqualTo(SourceKind.UPLOAD);
        assertThat(response.status()).isEqualTo(SourceStatus.READY);
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
        verify(sources).flush();
    }

    @Test
    void flushesAnAgentNoteBeforeReturningItsTimestampedResponse() {
        when(vaultService.accessibleVault(userId, vaultId)).thenReturn(vault(true));

        SourceResponse response = service.saveAgentNote(userId, vaultId, "Architecture", "Nexo knowledge");

        assertThat(response.sourceKind()).isEqualTo(SourceKind.AGENT);
        assertThat(response.status()).isEqualTo(SourceStatus.READY);
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
        verify(sources).flush();
    }

    private KnowledgeVault vault(boolean writable) {
        return KnowledgeVault.builder()
                .id(vaultId)
                .ownerId(userId)
                .ownerType(VaultOwnerType.USER)
                .name("Nexo Knowledge Base")
                .scope(VaultScope.PERSONAL)
                .writable(writable)
                .archived(false)
                .build();
    }

    private KnowledgeSource persisted(KnowledgeSource source) {
        Instant now = Instant.parse("2026-08-27T00:00:00Z");
        return KnowledgeSource.builder()
                .id(source.getId())
                .vaultId(source.getVaultId())
                .sourceKind(source.getSourceKind())
                .displayName(source.getDisplayName())
                .mimeType(source.getMimeType())
                .contentHash(source.getContentHash())
                .byteSize(source.getByteSize())
                .status(source.getStatus())
                .archived(source.isArchived())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
