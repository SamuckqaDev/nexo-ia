package com.nexoia.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.knowledge.retrieval.tool.KnowledgeSearchToolFactory;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.dto.ToolExecutionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnowledgeAnswerGroundingTest {

    private final KnowledgeAnswerGrounding grounding = new KnowledgeAnswerGrounding();

    @Test
    void replacesAnExternalLinkThatWasNotReturnedByTheVault() {
        CitationResponse citation = citation(
                "Nexo Product Vision",
                "Nexo keeps private knowledge isolated by user.");
        ChatCompletionOutcome outcome = outcome(
                "Centro de Suporte Nexo: https://support.nexo.com/unrelated",
                ToolExecutionStatus.FOUND,
                List.of(citation));

        ChatCompletionOutcome grounded = grounding.enforce(outcome);

        assertThat(grounded.content())
                .doesNotContain("support.nexo.com")
                .contains("link que não veio dos Knowledge Vaults")
                .contains("Nexo Product Vision")
                .contains("Nexo keeps private knowledge isolated by user")
                .contains("não forneceram uma URL verificável");
        assertThat(grounded.toolExecutions()).isEqualTo(outcome.toolExecutions());
    }

    @Test
    void keepsAUrlOnlyWhenItOccursVerbatimInTheRetrievedExcerpt() {
        String url = "https://docs.example.test/nexo";
        ChatCompletionOutcome outcome = outcome(
                "A fonte oficial é " + url + ".",
                ToolExecutionStatus.FOUND,
                List.of(citation("Nexo Principles", "Consulte " + url + " para detalhes.")));

        ChatCompletionOutcome grounded = grounding.enforce(outcome);

        assertThat(grounded.content()).isEqualTo(outcome.content());
    }

    @Test
    void replacesModelClaimsWhenTheVaultReturnedNoEvidence() {
        ChatCompletionOutcome outcome = outcome(
                "Encontrei uma central de suporte externa.",
                ToolExecutionStatus.NO_RESULTS,
                List.of());

        ChatCompletionOutcome grounded = grounding.enforce(outcome);

        assertThat(grounded.content())
                .isEqualTo("Não encontrei evidências relevantes nos Knowledge Vaults selecionados para "
                        + "responder a este pedido.");
    }

    private ChatCompletionOutcome outcome(
            String content,
            ToolExecutionStatus status,
            List<CitationResponse> citations) {
        ToolExecutionEvidence evidence = new ToolExecutionEvidence(
                UUID.randomUUID(),
                KnowledgeSearchToolFactory.TOOL_NAME,
                status,
                20,
                citations,
                Instant.parse("2026-08-26T21:00:00Z"));
        return new ChatCompletionOutcome(content, 100, 20, null, false, "stop", List.of(evidence));
    }

    private CitationResponse citation(String sourceName, String excerpt) {
        return new CitationResponse("Nexo Knowledge Base", sourceName, 1, excerpt, 0.9);
    }
}
