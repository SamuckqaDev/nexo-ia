package com.nexoia.conversation.inference.tool;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Builds a request-specific starter plan even when a small model ignores {@code update_plan}. */
@Component
public class AgentTaskDecomposer {

    private static final int MAX_STEPS = 8;
    private static final int MAX_STEP_LENGTH = 180;
    private static final String USER_REQUEST_MARKER = "\n[/NEXO_EXPLICIT_CONTEXT]\n\n[USER_REQUEST]\n";
    private static final Pattern LIST_BOUNDARY = Pattern.compile(
            "(?m)(?:^|\\n)\\s*(?:[-*•]|\\d+[.)])\\s+");
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("(?<=[.!?;])\\s+");
    private static final Pattern ACTION_BOUNDARY = Pattern.compile(
            "(?iu)(?:,\\s*(?:e\\s+)?|\\s+e\\s+)(?=(?:ajust|adicion|ativ|compar|conect|corrig|cri|"
                    + "faz|ger|habilit|implement|instal|lig|mont|mostr|padron|reduz|refator|remov|"
                    + "sub|us|valid|verific)\\p{L}*)");
    private static final Pattern FILLER = Pattern.compile(
            "(?iu)^(?:(?:cara|mano|por+ra|poh|por favor|mais um detalhe)[,:;.!?]*\\s*)+");

    public List<String> decompose(String objective) {
        String request = userRequest(objective);
        Set<String> candidates = new LinkedHashSet<>();
        for (String listed : LIST_BOUNDARY.split(request)) {
            for (String sentence : SENTENCE_BOUNDARY.split(listed)) {
                for (String clause : ACTION_BOUNDARY.split(sentence)) {
                    String normalized = normalize(clause);
                    if (meaningful(normalized)) {
                        candidates.add(normalized);
                    }
                }
            }
        }

        List<String> steps = new ArrayList<>(candidates.stream().limit(MAX_STEPS - 1L).toList());
        if (steps.isEmpty()) {
            steps.add("Executar o objetivo solicitado");
        }
        if (steps.size() == 1 && request.length() > 80) {
            steps.add("Revisar os detalhes e completar o resultado solicitado");
        }
        if (steps.size() < MAX_STEPS) {
            steps.add("Verificar o resultado e apresentar evidências");
        }
        return List.copyOf(steps);
    }

    private String userRequest(String objective) {
        if (objective == null || objective.isBlank()) {
            return "";
        }
        int marker = objective.indexOf(USER_REQUEST_MARKER);
        return marker < 0
                ? objective.trim()
                : objective.substring(marker + USER_REQUEST_MARKER.length()).trim();
    }

    private String normalize(String clause) {
        String value = FILLER.matcher(clause == null ? "" : clause.trim()).replaceFirst("")
                .replaceAll("\\s+", " ")
                .replaceAll("^[,;:.!?\\s]+|[,;:.!?\\s]+$", "")
                .trim();
        if (value.isEmpty()) {
            return value;
        }
        String bounded = value.length() <= MAX_STEP_LENGTH
                ? value
                : value.substring(0, MAX_STEP_LENGTH - 1).stripTrailing() + "…";
        return bounded.substring(0, 1).toUpperCase(Locale.ROOT) + bounded.substring(1);
    }

    private boolean meaningful(String value) {
        return value.length() >= 6
                && !value.equalsIgnoreCase("e")
                && !value.equalsIgnoreCase("também")
                && !value.equalsIgnoreCase("also");
    }
}
