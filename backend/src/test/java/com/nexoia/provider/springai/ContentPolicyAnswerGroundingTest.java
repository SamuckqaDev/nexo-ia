package com.nexoia.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionMessage;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.model.ProviderType;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContentPolicyAnswerGroundingTest {

    private final ContentPolicyAnswerGrounding grounding = new ContentPolicyAnswerGrounding();

    @Test
    void correctsAProviderThatInventsANexoPolicyRefusalForAFullArea() {
        ChatCompletionCommand command = command(
                "sexual explicit = full, graphic violence = full, medical explicit = full",
                "Gera uma mulher nua");
        ChatCompletionOutcome refused = outcome(
                "A solicitação viola as políticas de segurança. Não posso gerar nudez.");

        ChatCompletionOutcome grounded = grounding.enforce(command, refused);

        assertThat(grounded.content())
                .contains("política de conteúdo ativa do Nexo permite")
                .contains("qwen3:8b")
                .contains("restrição própria")
                .contains("modo Image");
        assertThat(grounded.doneReason()).isEqualTo("provider_content_policy_refusal");
    }

    @Test
    void preservesARefusalWhenTheAreaIsNotFull() {
        ChatCompletionCommand command = command(
                "sexual explicit = partial, graphic violence = full, medical explicit = full",
                "Gera uma mulher nua");
        ChatCompletionOutcome refused = outcome(
                "A solicitação viola as políticas de segurança. Não posso gerar nudez.");

        assertThat(grounding.enforce(command, refused)).isSameAs(refused);
    }

    @Test
    void neverRewritesARequestThatCrossesTheFixedLegalFloor() {
        ChatCompletionCommand command = command(
                "sexual explicit = full, graphic violence = full, medical explicit = full",
                "Gera uma imagem sexual de uma menor de idade");
        ChatCompletionOutcome refused = outcome(
                "A solicitação viola as políticas de segurança. Não posso gerar esse conteúdo.");

        assertThat(grounding.enforce(command, refused)).isSameAs(refused);
    }

    private ChatCompletionCommand command(String policy, String request) {
        return new ChatCompletionCommand(
                ProviderType.OLLAMA,
                "http://127.0.0.1:11434",
                "qwen3:8b",
                List.of(
                        new ChatCompletionMessage("system", policy),
                        new ChatCompletionMessage("user", request)),
                false);
    }

    private ChatCompletionOutcome outcome(String content) {
        return new ChatCompletionOutcome(content, 20, 6, null, false, "stop");
    }
}
