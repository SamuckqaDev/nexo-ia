package com.nexoia.conversation.inference.intent;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexoia.provider.dto.ChatCompletionMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserRequestIntentResolverTest {

    @Test
    void resolvesRepeatedConfirmationsToTheLastConcreteObjective() {
        List<ChatCompletionMessage> messages = List.of(
                new ChatCompletionMessage("user", "Crie um arquivo HTML com a paleta do projeto"),
                new ChatCompletionMessage("assistant", "Aqui está o código sugerido"),
                new ChatCompletionMessage("user", "Eu quero que você coloque na raiz do projeto"),
                new ChatCompletionMessage("assistant", "Execute estes comandos"),
                new ChatCompletionMessage("user", "Eu quero que vc faça isso'"),
                new ChatCompletionMessage("user", "faça"));

        assertThat(UserRequestIntentResolver.effectiveRequest(messages))
                .contains("coloque na raiz do projeto")
                .contains("User confirmation: faça");
        assertThat(UserRequestIntentResolver.requestsWorkspaceWrite(
                UserRequestIntentResolver.effectiveRequest(messages))).isTrue();
    }

    @Test
    void replacesAnIncompatibleSkillEnvelopeWithoutLosingTheConcreteObjective() {
        String skillEnvelope = """
                [NEXO_EXPLICIT_CONTEXT]
                Skill: Research brief
                [/NEXO_EXPLICIT_CONTEXT]

                [USER_REQUEST]
                faça
                """;
        List<ChatCompletionMessage> resolved = UserRequestIntentResolver.resolveContinuation(List.of(
                new ChatCompletionMessage("user", "Coloque o arquivo HTML na raiz do projeto"),
                new ChatCompletionMessage("assistant", "Use cat para criar o arquivo"),
                new ChatCompletionMessage("user", skillEnvelope)));

        assertThat(resolved.getLast().content())
                .contains("Coloque o arquivo HTML na raiz do projeto")
                .contains("Any selected Skill is auxiliary context only")
                .doesNotContain("Research brief");
    }

    @Test
    void doesNotMistakeAReadOnlyProjectDescriptionForAWriteRequest() {
        assertThat(UserRequestIntentResolver.requestsWorkspaceWrite(
                "Descreva a estrutura do projeto e os arquivos importantes"))
                .isFalse();
    }
}
