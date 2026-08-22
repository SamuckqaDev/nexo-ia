package com.nexoia.knowledge.retrieval.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.knowledge.retrieval.dto.RetrievalQuery;
import com.nexoia.knowledge.retrieval.dto.RetrievalResult;
import com.nexoia.knowledge.retrieval.service.RetrievalService;
import com.nexoia.provider.dto.KnowledgeToolScope;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.dto.ToolExecutionObserver;
import com.nexoia.provider.dto.ToolExecutionStarted;
import com.nexoia.provider.dto.ToolExecutionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeSearchToolFactoryTest {

    @Mock private RetrievalService retrieval;
    @Mock private AuditService audit;

    private KnowledgeSearchToolFactory factory;
    private final UUID userId = UUID.randomUUID();
    private final UUID vaultId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        factory = new KnowledgeSearchToolFactory(
                retrieval,
                audit,
                Clock.fixed(Instant.parse("2026-08-21T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void searchesOnlyTheServerAuthorizedVaultScopeAndCapturesEvidence() {
        CitationResponse citation = new CitationResponse(
                "Nexo KB", "Principles", 1, "Nexo is truthful.", 0.91);
        when(retrieval.retrieve(any(), any())).thenReturn(new RetrievalResult(List.of(citation)));
        RecordingObserver observer = new RecordingObserver();
        KnowledgeSearchToolSession session = factory.open(scope(), observer, () -> false);

        String result = session.callback().call("{\"query\":\"Nexo identity\",\"limit\":2}");

        ArgumentCaptor<RetrievalQuery> query = ArgumentCaptor.forClass(RetrievalQuery.class);
        verify(retrieval).retrieve(org.mockito.ArgumentMatchers.eq(userId), query.capture());
        assertThat(query.getValue().vaultIds()).containsExactly(vaultId);
        assertThat(result).contains("FOUND").contains("Nexo is truthful");
        assertThat(observer.started.argumentsDigest()).hasSize(64).doesNotContain("Nexo identity");
        assertThat(observer.completed.status()).isEqualTo(ToolExecutionStatus.FOUND);
        assertThat(session.evidence()).containsExactly(observer.completed);
    }

    @Test
    void deniesAnIdenticalRepeatedCallInsteadOfRunningAnUnboundedLoop() {
        when(retrieval.retrieve(any(), any())).thenReturn(RetrievalResult.empty());
        KnowledgeSearchToolSession session = factory.open(
                scope(), ToolExecutionObserver.NOOP, () -> false);

        session.callback().call("{\"query\":\"same question\"}");
        String repeated = session.callback().call("{\"query\":\"same question\"}");

        assertThat(repeated).contains("DENIED");
        assertThat(session.evidence()).extracting(ToolExecutionEvidence::status)
                .containsExactly(ToolExecutionStatus.NO_RESULTS, ToolExecutionStatus.DENIED);
    }

    private KnowledgeToolScope scope() {
        return new KnowledgeToolScope(
                userId, UUID.randomUUID(), UUID.randomUUID(), List.of(vaultId));
    }

    private static final class RecordingObserver implements ToolExecutionObserver {
        private ToolExecutionStarted started;
        private ToolExecutionEvidence completed;

        @Override
        public void onStarted(ToolExecutionStarted event) {
            started = event;
        }

        @Override
        public void onCompleted(ToolExecutionEvidence event) {
            completed = event;
        }
    }
}
