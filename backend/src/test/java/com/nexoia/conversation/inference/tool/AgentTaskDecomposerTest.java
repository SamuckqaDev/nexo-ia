package com.nexoia.conversation.inference.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

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

    @Test
    void expandsAProjectAnalysisIntoConcreteWorkspaceActions() {
        assertThat(decomposer.decompose("Cara analisa esse projeto para mim"))
                .extracting(AgentTaskDraft::title, AgentTaskDraft::requiredToolPrefix)
                .containsExactly(
                        tuple("Confirmar o projeto selecionado", null),
                        tuple(
                                "Mapear a estrutura do Workspace", "workspace_list_files"),
                        tuple(
                                "Identificar stack e repositório", "workspace_inspect_project"),
                        tuple(
                                "Verificar o estado Git", "workspace_git_status"),
                        tuple(
                                "Ler a documentação e o manifest principal", "workspace_read_file"),
                        tuple("Consolidar diagnóstico e prioridades", null));
    }

    @Test
    void expandsAFileCreationIntoAWriteAndVerificationPlan() {
        assertThat(decomposer.decompose("Crie hello.html na raiz do projeto"))
                .extracting(AgentTaskDraft::title, AgentTaskDraft::requiredToolPrefix)
                .containsExactly(
                        tuple("Confirmar a alteração solicitada", null),
                        tuple("Preparar a alteração no servidor", "workspace_create_file"),
                        tuple("Solicitar aprovação do diff", "workspace_create_file"),
                        tuple("Aplicar com revalidação", "workspace_create_file"),
                        tuple("Apresentar o resultado", null));
    }
}
