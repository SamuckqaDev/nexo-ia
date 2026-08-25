package com.nexoia.knowledge.retrieval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nexoia.knowledge.embedding.dto.EmbeddingOutcome;
import com.nexoia.knowledge.embedding.exception.EmbeddingProviderUnavailableException;
import com.nexoia.knowledge.embedding.service.EmbeddingService;
import com.nexoia.knowledge.ingestion.model.KnowledgeSource;
import com.nexoia.knowledge.ingestion.repository.SourceRepository;
import com.nexoia.knowledge.retrieval.config.KnowledgeRetrievalProperties;
import com.nexoia.knowledge.retrieval.dto.RetrievalQuery;
import com.nexoia.knowledge.retrieval.dto.RetrievalResult;
import com.nexoia.knowledge.retrieval.exception.RetrievalBudgetExceededException;
import com.nexoia.knowledge.retrieval.model.KnowledgeChunk;
import com.nexoia.knowledge.retrieval.repository.KnowledgeChunkRepository;
import com.nexoia.knowledge.vault.model.KnowledgeVault;
import com.nexoia.knowledge.vault.model.VaultScope;
import com.nexoia.knowledge.vault.repository.VaultRepository;
import com.nexoia.team.service.TeamMembershipService;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private VaultRepository vaults;
    @Mock
    private SourceRepository sources;
    @Mock
    private KnowledgeChunkRepository chunks;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private TeamMembershipService teamMembershipService;
    private RetrievalService service;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID vaultId = UUID.randomUUID();
    private final UUID sourceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RetrievalService(vaults, sources, chunks, embeddingService,
                new KnowledgeRetrievalProperties(6, 0.55, 2000), teamMembershipService);
    }

    @Test
    void rejectsMoreVaultsThanTheRetrievalBudgetAllows() {
        List<UUID> tooMany = nRandomIds(9);

        assertThatThrownBy(() -> service.retrieve(ownerId, new RetrievalQuery(tooMany, "question")))
                .isInstanceOf(RetrievalBudgetExceededException.class);
        verifyNoInteractions(embeddingService, chunks);
    }

    @Test
    void neverQueriesChunksForAVaultTheCallerDoesNotOwn() {
        when(vaults.findAllByOwnerIdInAndArchivedFalseAndIdIn(any(), any()))
                .thenReturn(List.of());

        RetrievalResult result = service.retrieve(ownerId, new RetrievalQuery(List.of(vaultId), "question"));

        assertThat(result.hasCitations()).isFalse();
        verifyNoInteractions(embeddingService, chunks);
    }

    @Test
    void neverFabricatesAnAnswerWhenTheEmbeddingProviderIsUnavailable() {
        when(vaults.findAllByOwnerIdInAndArchivedFalseAndIdIn(any(), any()))
                .thenReturn(List.of(vault(VaultScope.PERSONAL)));
        when(embeddingService.embed(any(), anyList())).thenThrow(new EmbeddingProviderUnavailableException());

        assertThatThrownBy(() -> service.retrieve(ownerId, new RetrievalQuery(List.of(vaultId), "question")))
                .isInstanceOf(EmbeddingProviderUnavailableException.class);
        verify(chunks, never()).findAuthorizedNearest(any(), anyList(), any(), any());
    }

    @Test
    void returnsAnExplicitEmptyResultBelowTheMinimumScore() {
        when(vaults.findAllByOwnerIdInAndArchivedFalseAndIdIn(any(), any()))
                .thenReturn(List.of(vault(VaultScope.PERSONAL)));
        when(embeddingService.embed(any(), anyList()))
                .thenReturn(new EmbeddingOutcome(List.of(new float[]{1f, 0f, 0f}), "nomic-embed-text", 3));
        when(chunks.findAuthorizedNearest(any(), anyList(), any(), any()))
                .thenReturn(List.of(chunk(new float[]{0f, 1f, 0f})));

        RetrievalResult result = service.retrieve(ownerId, new RetrievalQuery(List.of(vaultId), "question"));

        assertThat(result.hasCitations()).isFalse();
    }

    @Test
    void mapsAnAuthorizedChunkToACitationWithVaultAndSourceNames() {
        when(vaults.findAllByOwnerIdInAndArchivedFalseAndIdIn(any(), any()))
                .thenReturn(List.of(vault(VaultScope.PERSONAL)));
        when(embeddingService.embed(any(), anyList()))
                .thenReturn(new EmbeddingOutcome(List.of(new float[]{1f, 0f, 0f}), "nomic-embed-text", 3));
        when(chunks.findAuthorizedNearest(any(), anyList(), any(), any()))
                .thenReturn(List.of(chunk(new float[]{1f, 0f, 0f})));
        when(sources.findAllById(List.of(sourceId))).thenReturn(List.of(source()));

        RetrievalResult result = service.retrieve(ownerId, new RetrievalQuery(List.of(vaultId), "question"));

        assertThat(result.citations()).hasSize(1);
        assertThat(result.citations().getFirst().vaultName()).isEqualTo("Research");
        assertThat(result.citations().getFirst().sourceDisplayName()).isEqualTo("notes.md");
        assertThat(result.citations().getFirst().score()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    @SuppressWarnings("unchecked")
    void authorizesVaultsOwnedByTheUsersTeamsAlongsideTheirOwn() {
        UUID teamId = UUID.randomUUID();
        when(teamMembershipService.accessibleOwnerIds(ownerId)).thenReturn(List.of(ownerId, teamId));
        ArgumentCaptor<Iterable<UUID>> vaultOwnerSet = ArgumentCaptor.forClass(Iterable.class);
        when(vaults.findAllByOwnerIdInAndArchivedFalseAndIdIn(vaultOwnerSet.capture(), any()))
                .thenReturn(List.of(vault(VaultScope.PERSONAL)));
        when(embeddingService.embed(any(), anyList()))
                .thenReturn(new EmbeddingOutcome(List.of(new float[]{1f, 0f, 0f}), "nomic-embed-text", 3));
        ArgumentCaptor<Collection<UUID>> chunkOwnerSet = ArgumentCaptor.forClass(Collection.class);
        when(chunks.findAuthorizedNearest(chunkOwnerSet.capture(), anyList(), any(), any()))
                .thenReturn(List.of());

        service.retrieve(ownerId, new RetrievalQuery(List.of(vaultId), "question"));

        // Both authorization gates widen to include the user's Team, never another Team.
        assertThat(vaultOwnerSet.getValue()).contains(ownerId, teamId);
        assertThat(chunkOwnerSet.getValue()).contains(ownerId, teamId);
    }

    private List<UUID> nRandomIds(int count) {
        return IntStream.range(0, count).mapToObj(index -> UUID.randomUUID()).toList();
    }

    private KnowledgeVault vault(VaultScope scope) {
        return KnowledgeVault.builder().id(vaultId).ownerId(ownerId).name("Research").scope(scope)
                .archived(false).build();
    }

    private KnowledgeSource source() {
        return KnowledgeSource.builder().id(sourceId).vaultId(vaultId).displayName("notes.md")
                .archived(false).build();
    }

    private KnowledgeChunk chunk(float[] embedding) {
        return KnowledgeChunk.builder().id(UUID.randomUUID()).sourceId(sourceId).ordinal(0)
                .content("Some retrieved excerpt").tokenEstimate(5).embedding(embedding)
                .embeddingModel("nomic-embed-text").embeddingDimensions(embedding.length).build();
    }
}
