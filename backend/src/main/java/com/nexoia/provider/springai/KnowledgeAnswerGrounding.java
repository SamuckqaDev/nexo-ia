package com.nexoia.provider.springai;

import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.knowledge.retrieval.tool.KnowledgeSearchToolFactory;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Enforces claims that cannot be delegated reliably to a small local model after Vault retrieval. */
final class KnowledgeAnswerGrounding {

    private static final Pattern HTTP_URL = Pattern.compile("(?i)https?://[^\\s<>\\[\\]\\\"']+");
    private static final int MAX_RENDERED_EXCERPT = 360;

    ChatCompletionOutcome enforce(ChatCompletionOutcome outcome) {
        List<ToolExecutionEvidence> searches = outcome.toolExecutions().stream()
                .filter(execution -> KnowledgeSearchToolFactory.TOOL_NAME.equals(execution.toolName()))
                .toList();
        if (searches.isEmpty()) {
            return outcome;
        }

        List<CitationResponse> citations = searches.stream()
                .flatMap(execution -> execution.citations().stream())
                .distinct()
                .toList();
        if (citations.isEmpty()) {
            return replace(outcome,
                    "Não encontrei evidências relevantes nos Knowledge Vaults selecionados para "
                            + "responder a este pedido.");
        }

        Set<String> evidenceUrls = new LinkedHashSet<>();
        citations.forEach(citation -> evidenceUrls.addAll(urls(citation.excerpt())));
        Set<String> unsupportedUrls = urls(outcome.content());
        unsupportedUrls.removeAll(evidenceUrls);
        if (unsupportedUrls.isEmpty()) {
            return outcome;
        }

        return replace(outcome, verifiedEvidence(citations));
    }

    private ChatCompletionOutcome replace(ChatCompletionOutcome outcome, String content) {
        return new ChatCompletionOutcome(
                content,
                outcome.inputTokens(),
                outcome.outputTokens(),
                outcome.tokenSource(),
                outcome.cancelled(),
                outcome.doneReason(),
                outcome.toolExecutions());
    }

    private String verifiedEvidence(List<CitationResponse> citations) {
        StringBuilder answer = new StringBuilder(
                "A resposta do modelo foi descartada porque incluiu um link que não veio dos "
                        + "Knowledge Vaults. Evidências verificadas:\n");
        for (CitationResponse citation : citations) {
            answer.append("\n- **")
                    .append(singleLine(citation.sourceDisplayName()))
                    .append("** — Vault `")
                    .append(singleLine(citation.vaultName()))
                    .append("`, trecho ")
                    .append(citation.chunkOrdinal())
                    .append(": ")
                    .append(boundedExcerpt(citation.excerpt()));
        }
        answer.append("\n\nOs Vaults consultados não forneceram uma URL verificável para essas evidências.");
        return answer.toString();
    }

    private String boundedExcerpt(String value) {
        String excerpt = singleLine(value);
        return excerpt.length() <= MAX_RENDERED_EXCERPT
                ? excerpt
                : excerpt.substring(0, MAX_RENDERED_EXCERPT) + "…";
    }

    private String singleLine(String value) {
        return Objects.requireNonNullElse(value, "").replaceAll("\\s+", " ").trim();
    }

    private Set<String> urls(String value) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = HTTP_URL.matcher(Objects.requireNonNullElse(value, ""));
        while (matcher.find()) {
            found.add(stripTrailingPunctuation(matcher.group()));
        }
        return found;
    }

    private String stripTrailingPunctuation(String value) {
        int end = value.length();
        while (end > 0 && ".,;:!?)]}".indexOf(value.charAt(end - 1)) >= 0) {
            end--;
        }
        return value.substring(0, end);
    }
}
