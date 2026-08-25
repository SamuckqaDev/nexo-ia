package com.nexoia.knowledge.ingestion.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.knowledge.embedding.exception.EmbeddingProviderUnavailableException;
import com.nexoia.knowledge.ingestion.service.SourceIngestionService;
import com.nexoia.knowledge.vault.exception.VaultNotWritableException;
import com.nexoia.provider.dto.KnowledgeWriteToolScope;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.dto.ToolExecutionObserver;
import com.nexoia.provider.dto.ToolExecutionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeWriteToolFactoryTest {

    @Mock private SourceIngestionService ingestion;
    @Mock private AuditService audit;

    private KnowledgeWriteToolFactory factory;
    private final UUID userId = UUID.randomUUID();
    private final UUID vaultId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        factory = new KnowledgeWriteToolFactory(
                ingestion, audit, Clock.fixed(Instant.parse("2026-08-21T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void appendsToTheServerResolvedVaultAndCapturesEvidenceWithoutLeakingContent() {
        KnowledgeWriteToolSession session = factory.open(scope(vaultId), ToolExecutionObserver.NOOP, () -> false);

        String result = session.callback().call(
                "{\"title\":\"Deploy step\",\"content\":\"Run mvnw verify before release.\"}");

        verify(ingestion).saveAgentNote(eq(userId), eq(vaultId), eq("Deploy step"),
                eq("Run mvnw verify before release."));
        assertThat(result).contains("COMPLETED").contains("Nexo KB");
        assertThat(session.evidence()).extracting(ToolExecutionEvidence::status)
                .containsExactly(ToolExecutionStatus.COMPLETED);
    }

    @Test
    void deniesWhenNoWritableVaultTargetWasResolved() {
        KnowledgeWriteToolSession session = factory.open(scope(null), ToolExecutionObserver.NOOP, () -> false);

        String result = session.callback().call("{\"title\":\"T\",\"content\":\"C\"}");

        assertThat(result).contains("DENIED");
        verify(ingestion, never()).saveAgentNote(any(), any(), any(), any());
    }

    @Test
    void deniesWhenTheVaultIsNotWritable() {
        when(ingestion.saveAgentNote(any(), any(), any(), any())).thenThrow(new VaultNotWritableException());
        KnowledgeWriteToolSession session = factory.open(scope(vaultId), ToolExecutionObserver.NOOP, () -> false);

        String result = session.callback().call("{\"title\":\"T\",\"content\":\"C\"}");

        assertThat(result).contains("DENIED").contains("not writable");
        assertThat(session.evidence()).extracting(ToolExecutionEvidence::status)
                .containsExactly(ToolExecutionStatus.DENIED);
    }

    @Test
    void reportsUnavailableWhenEmbeddingProviderIsDown() {
        when(ingestion.saveAgentNote(any(), any(), any(), any()))
                .thenThrow(new EmbeddingProviderUnavailableException());
        KnowledgeWriteToolSession session = factory.open(scope(vaultId), ToolExecutionObserver.NOOP, () -> false);

        String result = session.callback().call("{\"title\":\"T\",\"content\":\"C\"}");

        assertThat(result).contains("UNAVAILABLE");
    }

    @Test
    void deniesAnIdenticalRepeatedSaveInsteadOfDuplicating() {
        KnowledgeWriteToolSession session = factory.open(scope(vaultId), ToolExecutionObserver.NOOP, () -> false);

        session.callback().call("{\"title\":\"Same\",\"content\":\"Same body\"}");
        String repeated = session.callback().call("{\"title\":\"Same\",\"content\":\"Same body\"}");

        assertThat(repeated).contains("DENIED");
        assertThat(session.evidence()).extracting(ToolExecutionEvidence::status)
                .containsExactly(ToolExecutionStatus.COMPLETED, ToolExecutionStatus.DENIED);
        verify(ingestion).saveAgentNote(any(), any(), any(), any());
    }

    private KnowledgeWriteToolScope scope(UUID vault) {
        return new KnowledgeWriteToolScope(userId, vault, "Nexo KB", UUID.randomUUID(), UUID.randomUUID());
    }
}
