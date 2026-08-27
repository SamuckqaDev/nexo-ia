package com.nexoia.conversation.inference.intent;

import com.nexoia.provider.dto.ChatCompletionMessage;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Resolves short confirmations against the latest concrete user objective. */
public final class UserRequestIntentResolver {

    private static final String USER_REQUEST_MARKER =
            "\n[/NEXO_EXPLICIT_CONTEXT]\n\n[USER_REQUEST]\n";
    private static final Pattern CONTINUATION = Pattern.compile(
            "(?:(?:eu\\s+quero\\s+que\\s+(?:voce|vc)\\s+)?|(?:pode\\s+)?)?"
                    + "(?:faca|faz|execute|executa|aplique|aplica|continue|continuar?|continua|"
                    + "prossiga|segue|siga)(?:\\s+(?:isso|agora|aqui|dai|daqui))?");
    private static final Pattern WORKSPACE_WRITE_ACTION = Pattern.compile(
            "\\b(?:crie|cria|criar|adicione|adicionar|coloque|colocar|salve|salvar|"
                    + "escreva|escrever|write|create|add|altere|alterar|edite|editar|"
                    + "modifique|modificar|corrija|corrigir|fix|implemente|implementar|"
                    + "refatore|refatorar|remova|remover|delete|mova|mover|renomeie|"
                    + "rename|aplique|apply|construa|construir|faca)\\b");
    private static final Pattern WORKSPACE_WRITE_TARGET = Pattern.compile(
            "(?:\\b(?:arquivo|file|projeto|workspace|repositorio|codigo|code|pasta|"
                    + "diretorio|raiz)\\b|\\.(?:html|css|js|ts|tsx|jsx|java|kt|py|md|"
                    + "json|ya?ml|xml|sql|sh|ps1|properties)\\b)");

    private UserRequestIntentResolver() {}

    public static String effectiveRequest(List<ChatCompletionMessage> messages) {
        List<String> requests = messages.reversed().stream()
                .filter(message -> "user".equalsIgnoreCase(message.role()))
                .map(ChatCompletionMessage::content)
                .filter(Objects::nonNull)
                .map(UserRequestIntentResolver::explicitRequest)
                .filter(request -> !request.isBlank())
                .toList();
        if (requests.isEmpty()) {
            return "";
        }
        String latest = requests.getFirst();
        if (!isContinuation(latest)) {
            return latest;
        }
        return requests.stream()
                .skip(1)
                .filter(request -> !isContinuation(request))
                .findFirst()
                .map(previous -> previous + "\n\nUser confirmation: " + latest)
                .orElse(latest);
    }

    public static List<ChatCompletionMessage> resolveContinuation(List<ChatCompletionMessage> messages) {
        int latestUserIndex = -1;
        String latest = "";
        for (int index = messages.size() - 1; index >= 0; index--) {
            ChatCompletionMessage message = messages.get(index);
            if ("user".equalsIgnoreCase(message.role()) && message.content() != null) {
                latestUserIndex = index;
                latest = explicitRequest(message.content());
                break;
            }
        }
        if (latestUserIndex < 0 || !isContinuation(latest)) {
            return messages;
        }
        String previous = "";
        for (int index = latestUserIndex - 1; index >= 0; index--) {
            ChatCompletionMessage message = messages.get(index);
            if (!"user".equalsIgnoreCase(message.role()) || message.content() == null) {
                continue;
            }
            String candidate = explicitRequest(message.content());
            if (!candidate.isBlank() && !isContinuation(candidate)) {
                previous = candidate;
                break;
            }
        }
        if (previous.isBlank()) {
            return messages;
        }
        List<ChatCompletionMessage> resolved = new ArrayList<>(messages);
        resolved.set(latestUserIndex, new ChatCompletionMessage("user", """
                Continue and execute this unresolved user objective now:
                %s

                The user's current confirmation is: %s
                Any selected Skill is auxiliary context only; it must not replace this objective.
                """.formatted(previous, latest).strip()));
        return List.copyOf(resolved);
    }

    public static boolean requestsWorkspaceWrite(String request) {
        String normalized = normalize(request);
        return WORKSPACE_WRITE_ACTION.matcher(normalized).find()
                && WORKSPACE_WRITE_TARGET.matcher(normalized).find();
    }

    public static boolean isContinuation(String request) {
        String compact = normalize(explicitRequest(request))
                .replaceAll("[.!?,;:'\"`“”‘’]+$", "")
                .trim();
        return CONTINUATION.matcher(compact).matches();
    }

    public static String explicitRequest(String request) {
        int marker = request.indexOf(USER_REQUEST_MARKER);
        return marker < 0
                ? request.trim()
                : request.substring(marker + USER_REQUEST_MARKER.length()).trim();
    }

    public static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
