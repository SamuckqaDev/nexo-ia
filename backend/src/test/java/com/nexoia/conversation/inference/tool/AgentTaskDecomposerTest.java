package com.nexoia.conversation.inference.tool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AgentTaskDecomposerTest {

    private final AgentTaskDecomposer decomposer = new AgentTaskDecomposer();

    @Test
    void dividesACompoundObjectiveIntoSmallVerifiableSteps() {
        assertThat(decomposer.decompose(
                "Cara corrige os botões, ajusta o plano e conecta o Docker MCP"))
                .containsExactly(
                        "Corrige os botões",
                        "Ajusta o plano",
                        "Conecta o Docker MCP",
                        "Verificar o resultado e apresentar evidências");
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
                .containsExactly(
                        "Implemente a busca",
                        "Valide o resultado",
                        "Verificar o resultado e apresentar evidências");
    }
}
