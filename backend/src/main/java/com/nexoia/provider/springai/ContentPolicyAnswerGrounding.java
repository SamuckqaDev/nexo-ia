package com.nexoia.provider.springai;

import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionMessage;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/**
 * Prevents a provider model from falsely attributing its own topical refusal to Nexo's resolved
 * content policy. The deterministic permission matrix remains authoritative; this guard does not
 * force a model to generate content and never relaxes the fixed legal floor.
 */
final class ContentPolicyAnswerGrounding {

    boolean shouldBuffer(ChatCompletionCommand command) {
        String request = latestUserRequest(command);
        SensitiveArea area = SensitiveArea.detect(request);
        if (area == null || crossesFixedLegalFloor(request)) {
            return false;
        }
        return systemContext(command).contains(area.policyKey() + " = full");
    }

    ChatCompletionOutcome enforce(
            ChatCompletionCommand command,
            ChatCompletionOutcome outcome) {
        if (outcome.content() == null || outcome.content().isBlank()) {
            return outcome;
        }

        String answer = normalize(outcome.content());
        String request = latestUserRequest(command);
        String systemContext = systemContext(command);
        SensitiveArea area = SensitiveArea.detect(request);
        if (area == null
                || !isPolicyAttributedRefusal(answer)
                || !systemContext.contains(area.policyKey() + " = full")
                || crossesFixedLegalFloor(request)) {
            return outcome;
        }

        String correction = "A política de conteúdo ativa do Nexo permite este pedido lícito na área "
                + area.displayName()
                + ". O modelo selecionado (`" + command.model() + "`) recusou por uma restrição própria; "
                + "essa recusa não veio da política configurada no Nexo. ";
        if (asksForImage(request)) {
            correction += "Nenhuma imagem foi gerada nesta execução. Use o modo Image com um modelo de "
                    + "imagem compatível ou selecione outro modelo que aceite essa política.";
        } else {
            correction += "Selecione outro modelo compatível para executar o pedido.";
        }
        return new ChatCompletionOutcome(
                correction,
                outcome.inputTokens(),
                outcome.outputTokens(),
                outcome.tokenSource(),
                outcome.cancelled(),
                "provider_content_policy_refusal",
                outcome.toolExecutions());
    }

    private boolean isPolicyAttributedRefusal(String answer) {
        return answer.contains("viola as politica")
                || answer.contains("viola politica")
                || answer.contains("politicas de seguranca")
                || answer.contains("seguranca e etica")
                || answer.contains("conteudo inadequado")
                || answer.contains("conteudo ofensivo")
                || answer.contains("against policy")
                || answer.contains("violates policy")
                || answer.contains("not allowed by policy");
    }

    private boolean crossesFixedLegalFloor(String request) {
        return request.contains("crianca")
                || request.contains("menor de idade")
                || request.contains("adolescente")
                || request.contains("child")
                || request.contains("minor")
                || request.contains("underage");
    }

    private boolean asksForImage(String request) {
        return request.contains("imagem")
                || request.contains("foto")
                || request.contains("desenh")
                || request.contains("ilustr")
                || request.contains("gera ")
                || request.startsWith("gera")
                || request.contains("generate an image");
    }

    private String latestUserRequest(ChatCompletionCommand command) {
        return command.messages().reversed().stream()
                .filter(message -> "user".equalsIgnoreCase(message.role()))
                .map(ChatCompletionMessage::content)
                .filter(Objects::nonNull)
                .map(this::normalize)
                .findFirst()
                .orElse("");
    }

    private String systemContext(ChatCompletionCommand command) {
        return command.messages().stream()
                .filter(message -> "system".equalsIgnoreCase(message.role()))
                .map(ChatCompletionMessage::content)
                .filter(Objects::nonNull)
                .map(this::normalize)
                .reduce("", (left, right) -> left + "\n" + right);
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private enum SensitiveArea {
        SEXUAL("sexual explicit", "conteúdo sexual explícito"),
        GRAPHIC_VIOLENCE("graphic violence", "violência gráfica"),
        MEDICAL("medical explicit", "conteúdo médico explícito");

        private final String policyKey;
        private final String displayName;

        SensitiveArea(String policyKey, String displayName) {
            this.policyKey = policyKey;
            this.displayName = displayName;
        }

        private String policyKey() {
            return policyKey;
        }

        private String displayName() {
            return displayName;
        }

        private static SensitiveArea detect(String request) {
            if (request.contains("nude")
                    || request.contains("nua")
                    || request.contains("nu ")
                    || request.contains("sexo")
                    || request.contains("sexual")) {
                return SEXUAL;
            }
            if (request.contains("fratura exposta")
                    || request.contains("cirurgia grafica")
                    || request.contains("medical explicit")) {
                return MEDICAL;
            }
            if (request.contains("violencia grafica")
                    || request.contains("ferimento grafico")
                    || request.contains("graphic violence")) {
                return GRAPHIC_VIOLENCE;
            }
            return null;
        }
    }
}
