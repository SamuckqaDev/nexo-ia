package com.nexoia.conversation.inference.tool;

import com.nexoia.workspace.tool.WorkspaceReadToolFactory;
import java.text.Normalizer;
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
            "(?iu)(?:,\\s*(?:e\\s+)?|\\s+e\\s+)(?=(?:ajust|adicion|ativ|compar|conect|consult|corrig|cri|"
                    + "faz|ger|guard|habilit|implement|instal|lembr|lig|mont|mostr|padron|reduz|refator|remov|"
                    + "sub|us|valid|verific)\\p{L}*)");
    private static final Pattern FILLER = Pattern.compile(
            "(?iu)^(?:(?:cara|mano|por+ra|poh|por favor|mais um detalhe)[,:;.!?]*\\s*)+");

    public List<AgentTaskDraft> decompose(String objective) {
        String request = userRequest(objective);
        if (isProjectAnalysis(request)) {
            return projectAnalysisSteps();
        }
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

        List<AgentTaskDraft> steps = new ArrayList<>();
        steps.add(new AgentTaskDraft(
                "Analisar a solicitação",
                "Confirmar o resultado pedido e as capacidades autorizadas para esta execução.",
                null));
        candidates.stream()
                .limit(MAX_STEPS - 3L)
                .map(step -> new AgentTaskDraft(
                        step,
                        "Executar esta parte do pedido e registrar o resultado observável.",
                        requiredToolPrefix(step)))
                .forEach(steps::add);
        if (steps.size() == 1) {
            steps.add(new AgentTaskDraft(
                    "Executar o objetivo solicitado",
                    "Produzir o resultado solicitado dentro das capacidades disponíveis.",
                    requiredToolPrefix(request)));
        }
        steps.add(new AgentTaskDraft(
                "Verificar ações e evidências",
                "Conferir o status real das ferramentas executadas e não declarar ações sem confirmação.",
                null));
        steps.add(new AgentTaskDraft(
                "Apresentar o resultado",
                "Entregar uma resposta objetiva, indicando evidências, limitações ou etapas pendentes.",
                null));
        return List.copyOf(steps);
    }

    private List<AgentTaskDraft> projectAnalysisSteps() {
        return List.of(
                new AgentTaskDraft(
                        "Confirmar o projeto selecionado",
                        "Validar o objetivo e o Workspace autorizado para esta análise.",
                        null),
                new AgentTaskDraft(
                        "Mapear a estrutura do Workspace",
                        "Listar a raiz real do projeto e registrar arquivos, pastas e omissões.",
                        WorkspaceReadToolFactory.LIST_FILES),
                new AgentTaskDraft(
                        "Identificar stack e repositório",
                        "Detectar manifests, branch e HEAD sem alterar o projeto.",
                        WorkspaceReadToolFactory.INSPECT_PROJECT),
                new AgentTaskDraft(
                        "Verificar o estado Git",
                        "Conferir alterações atuais para separar código versionado de trabalho pendente.",
                        WorkspaceReadToolFactory.GIT_STATUS),
                new AgentTaskDraft(
                        "Ler a documentação e o manifest principal",
                        "Usar arquivos reais do projeto para fundamentar arquitetura e dependências.",
                        WorkspaceReadToolFactory.READ_FILE),
                new AgentTaskDraft(
                        "Consolidar diagnóstico e prioridades",
                        "Entregar achados, riscos, limitações da evidência e próximos passos priorizados.",
                        null));
    }

    private boolean isProjectAnalysis(String request) {
        String normalized = Normalizer.normalize(request, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        boolean project = normalized.contains("projeto")
                || normalized.contains("workspace")
                || normalized.contains("repositorio")
                || normalized.contains("repository")
                || normalized.contains("codebase");
        boolean analysis = normalized.contains("analis")
                || normalized.contains("avali")
                || normalized.contains("diagnost")
                || normalized.contains("review")
                || normalized.contains("revise");
        return project && analysis;
    }

    private String requiredToolPrefix(String step) {
        String normalized = step.toLowerCase(Locale.ROOT);
        if (normalized.contains("memória") || normalized.contains("memoria")
                || normalized.contains("lembre") || normalized.contains("guarde")) {
            return "remember";
        }
        if (normalized.contains("base de conhecimento") || normalized.contains("knowledge")
                || normalized.contains("vault") || normalized.contains("conhecimento")) {
            return "search_knowledge";
        }
        if (normalized.contains("internet") || normalized.contains(" web")
                || normalized.contains("pesquis") || normalized.contains("busc")
                || normalized.contains("url") || normalized.contains("site")) {
            return "mcp_";
        }
        return null;
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
