package com.nexoia.conversation.inference.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AgentTaskDecomposerTest {

    private final AgentTaskDecomposer decomposer = new AgentTaskDecomposer();

    @Test
    void dividesACompoundObjectiveIntoSmallVerifiableSteps() {
        assertThat(decomposer.decompose(
                "Cara corrige os botões, ajusta o plano e conecta o Docker MCP"))
                .extracting(AgentTaskDraft::title)
                .containsExactly(
                        "Analisar a solicitação",
                        "Corrige os botões",
                        "Ajusta o plano",
                        "Conecta o Docker MCP",
                        "Verificar ações e evidências",
                        "Apresentar o resultado");
        assertThat(decomposer.decompose(
                "Cara corrige os botões, ajusta o plano e conecta o Docker MCP"))
                .allMatch(step -> !step.description().isBlank());
    }

    @Test
    void ignoresSerializedExplicitContextWhenBuildingThePlan() {
        String objective = """
                [NEXO_EXPLICIT_CONTEXT]
                {"workspace":{"name":"Nexo"}}
                [/NEXO_EXPLICIT_CONTEXT]

                [USER_REQUEST]
                Implemente a busca e valide o resultado
                """;

        assertThat(decomposer.decompose(objective))
                .extracting(AgentTaskDraft::title)
                .containsExactly(
                        "Analisar a solicitação",
                        "Implemente a busca",
                        "Valide o resultado",
                        "Verificar ações e evidências",
                        "Apresentar o resultado");
    }

    @Test
    void marksKnowledgeAndMemoryActionsAsEvidenceDependent() {
        assertThat(decomposer.decompose(
                "Consulte a base de conhecimento e guarde o resultado na memória"))
                .extracting(AgentTaskDraft::requiredToolPrefix)
                .contains("search_knowledge", "remember");
    }
}
