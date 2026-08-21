package com.nexoia.knowledge.graph.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.knowledge.graph.dto.KnowledgeGraphNodeKind;
import com.nexoia.knowledge.graph.dto.KnowledgeGraphRelation;
import com.nexoia.knowledge.ingestion.model.KnowledgeSource;
import com.nexoia.knowledge.ingestion.model.SourceStatus;
import com.nexoia.knowledge.ingestion.repository.SourceRepository;
import com.nexoia.knowledge.retrieval.model.KnowledgeChunk;
import com.nexoia.knowledge.retrieval.repository.KnowledgeChunkRepository;
import com.nexoia.knowledge.vault.model.KnowledgeVault;
import com.nexoia.knowledge.vault.model.VaultScope;
import com.nexoia.knowledge.vault.repository.VaultRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphServiceTest {

    @Mock
    private VaultRepository vaults;
    @Mock
    private SourceRepository sources;
    @Mock
    private KnowledgeChunkRepository chunks;
    private KnowledgeGraphService service;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID vaultId = UUID.randomUUID();
    private final UUID firstSourceId = UUID.randomUUID();
    private final UUID secondSourceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new KnowledgeGraphService(vaults, sources, chunks);
    }

    @Test
    void buildsARealGraphWithContainmentAndSemanticRelationships() {
        when(vaults.findAllByOwnerIdAndArchivedFalseOrderByUpdatedAtDesc(eq(ownerId), any(Limit.class)))
                .thenReturn(List.of(vault()));
        when(sources.findAuthorizedForGraph(eq(ownerId), eq(List.of(vaultId)), any(Limit.class)))
                .thenReturn(List.of(source(firstSourceId, "Vision.md"), source(secondSourceId, "Principles.md")));
        when(chunks.findAuthorizedForGraph(eq(ownerId), any(), any(Limit.class)))
                .thenReturn(List.of(
                        chunk(firstSourceId, 0, "Local-first workspace", new float[]{1f, 0f, 0f}),
                        chunk(secondSourceId, 0, "Private local knowledge", new float[]{0.98f, 0.1f, 0f})));

        var graph = service.graph(ownerId);

        assertThat(graph.vaultCount()).isEqualTo(1);
        assertThat(graph.sourceCount()).isEqualTo(2);
        assertThat(graph.chunkCount()).isEqualTo(2);
        assertThat(graph.nodes()).extracting(node -> node.kind())
                .containsExactlyInAnyOrder(
                        KnowledgeGraphNodeKind.VAULT,
                        KnowledgeGraphNodeKind.SOURCE,
                        KnowledgeGraphNodeKind.SOURCE,
                        KnowledgeGraphNodeKind.CHUNK,
                        KnowledgeGraphNodeKind.CHUNK);
        assertThat(graph.edges()).extracting(edge -> edge.relation())
                .contains(KnowledgeGraphRelation.CONTAINS, KnowledgeGraphRelation.SEMANTIC);
        assertThat(graph.edges().stream()
                .filter(edge -> edge.relation() == KnowledgeGraphRelation.SEMANTIC)
                .findFirst().orElseThrow().similarity()).isGreaterThan(0.9);
    }

    @Test
    void appliesTheAuthenticatedOwnerToEveryRepositoryBoundary() {
        when(vaults.findAllByOwnerIdAndArchivedFalseOrderByUpdatedAtDesc(eq(ownerId), any(Limit.class)))
                .thenReturn(List.of(vault()));
        when(sources.findAuthorizedForGraph(eq(ownerId), eq(List.of(vaultId)), any(Limit.class)))
                .thenReturn(List.of(source(firstSourceId, "Vision.md")));
        when(chunks.findAuthorizedForGraph(eq(ownerId), eq(List.of(firstSourceId)), any(Limit.class)))
                .thenReturn(List.of());

        service.graph(ownerId);

        verify(vaults).findAllByOwnerIdAndArchivedFalseOrderByUpdatedAtDesc(eq(ownerId), any(Limit.class));
        verify(sources).findAuthorizedForGraph(eq(ownerId), eq(List.of(vaultId)), any(Limit.class));
        verify(chunks).findAuthorizedForGraph(eq(ownerId), eq(List.of(firstSourceId)), any(Limit.class));
    }

    private KnowledgeVault vault() {
        return KnowledgeVault.builder()
                .id(vaultId)
                .ownerId(ownerId)
                .name("Nexo Knowledge Base")
                .scope(VaultScope.PERSONAL)
                .archived(false)
                .build();
    }

    private KnowledgeSource source(UUID id, String name) {
        return KnowledgeSource.builder()
                .id(id)
                .vaultId(vaultId)
                .displayName(name)
                .status(SourceStatus.READY)
                .archived(false)
                .build();
    }

    private KnowledgeChunk chunk(UUID sourceId, int ordinal, String content, float[] embedding) {
        return KnowledgeChunk.builder()
                .id(UUID.randomUUID())
                .sourceId(sourceId)
                .ordinal(ordinal)
                .content(content)
                .tokenEstimate(8)
                .embedding(embedding)
                .embeddingModel("nomic-embed-text")
                .embeddingDimensions(embedding.length)
                .build();
    }
}
