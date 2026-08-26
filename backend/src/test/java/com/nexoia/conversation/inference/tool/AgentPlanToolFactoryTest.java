package com.nexoia.conversation.inference.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexoia.audit.service.AuditService;
import com.nexoia.conversation.inference.model.AgentPlanStepStatus;
import com.nexoia.provider.dto.AgentPlanToolScope;
import com.nexoia.provider.dto.AgentPlanUpdate;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.dto.ToolExecutionObserver;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentPlanToolFactoryTest {

    @Mock private AuditService audit;

    private AgentPlanToolFactory factory;

    @BeforeEach
    void setUp() {
        factory = new AgentPlanToolFactory(
                audit,
                Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneOffset.UTC),
                new AgentTaskDecomposer());
    }

    @Test
    void persistsAValidatedVisiblePlanWithoutModelFacingUserIdentifiers() {
        List<AgentPlanUpdate> updates = new ArrayList<>();
        AgentPlanToolSession session = factory.open(
                scope(), ToolExecutionObserver.NOOP, updates::add, () -> false);

        String result = session.callback().call("""
                {"explanation":"Start with evidence", "plan":[
                  {"step":"Inspect the selected Vaults","status":"IN_PROGRESS"},
                  {"step":"Answer with citations","status":"PENDING"}
                ]}
                """);

        assertThat(result).contains("COMPLETED").contains("revision");
        assertThat(updates).hasSize(2);
        assertThat(updates.getFirst()).satisfies(update -> {
            assertThat(update.revision()).isEqualTo(1);
            assertThat(update.steps()).extracting(step -> step.step())
                    .containsExactly(
                            "Analisar a solicitação",
                            "Pesquisar as fontes disponíveis",
                            "Comparar os resultados encontrados",
                            "Verificar ações e evidências",
                            "Apresentar o resultado");
            assertThat(update.steps()).extracting(step -> step.description())
                    .allMatch(description -> description != null && !description.isBlank());
            assertThat(update.steps()).extracting(step -> step.status())
                    .containsExactly(
                            AgentPlanStepStatus.IN_PROGRESS,
                            AgentPlanStepStatus.PENDING,
                            AgentPlanStepStatus.PENDING,
                            AgentPlanStepStatus.PENDING,
                            AgentPlanStepStatus.PENDING);
        });
        assertThat(updates.getLast()).satisfies(update -> {
            assertThat(update.revision()).isEqualTo(2);
            assertThat(update.steps()).extracting(step -> step.step())
                    .containsExactly("Inspect the selected Vaults", "Answer with citations");
        });
        assertThat(session.evidence()).extracting(ToolExecutionEvidence::status)
                .containsExactly(ToolExecutionStatus.COMPLETED);
        assertThat(session.callback().getToolDefinition().inputSchema())
                .doesNotContain("userId", "assistantMessageId", "correlationId");
    }

    @Test
    void deniesARepeatedPlanAndPlansWithMultipleActiveSteps() {
        AgentPlanToolSession session = factory.open(
                scope(), ToolExecutionObserver.NOOP, update -> {}, () -> false);
        String valid = """
                {"plan":[{"step":"Inspect","status":"IN_PROGRESS"}]}
                """;

        session.callback().call(valid);
        String repeated = session.callback().call(valid);
        String invalid = session.callback().call("""
                {"plan":[
                  {"step":"Inspect","status":"IN_PROGRESS"},
                  {"step":"Implement","status":"IN_PROGRESS"}
                ]}
                """);

        assertThat(repeated).contains("DENIED");
        assertThat(invalid).contains("DENIED");
        assertThat(session.evidence()).extracting(ToolExecutionEvidence::status)
                .containsExactly(
                        ToolExecutionStatus.COMPLETED,
                        ToolExecutionStatus.DENIED,
                        ToolExecutionStatus.DENIED);
    }

    @Test
    void leavesEvidenceDependentFallbackStepsPendingWhenNoToolRan() {
        List<AgentPlanUpdate> updates = new ArrayList<>();
        AgentPlanToolSession session = factory.open(
                scope(), ToolExecutionObserver.NOOP, updates::add, () -> false);

        session.completeFallback();
        session.completeFallback();

        assertThat(updates).hasSize(2);
        assertThat(updates.getFirst().revision()).isEqualTo(1);
        assertThat(updates.getLast().revision()).isEqualTo(2);
        assertThat(updates.getLast().steps())
                .anyMatch(step -> step.status() == AgentPlanStepStatus.PENDING)
                .anyMatch(step -> step.status() == AgentPlanStepStatus.COMPLETED);
        assertThat(updates.getLast().explanation()).contains("without runtime evidence");
        assertThat(session.evidence()).isEmpty();
    }

    private AgentPlanToolScope scope() {
        return new AgentPlanToolScope(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Pesquisar as fontes disponíveis e comparar os resultados encontrados");
    }
}
