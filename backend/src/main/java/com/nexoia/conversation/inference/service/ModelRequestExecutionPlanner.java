package com.nexoia.conversation.inference.service;

import com.nexoia.conversation.chat.exception.ConversationNotFoundException;
import com.nexoia.conversation.chat.model.Conversation;
import com.nexoia.conversation.chat.model.ConversationMode;
import com.nexoia.conversation.chat.model.ConversationRole;
import com.nexoia.conversation.chat.model.MessageStatus;
import com.nexoia.conversation.chat.repository.ConversationMessageRepository;
import com.nexoia.conversation.chat.repository.ConversationRepository;
import com.nexoia.conversation.inference.dto.ModelRequestExecutionPlan;
import com.nexoia.conversation.inference.exception.AgentCapableModelUnavailableException;
import com.nexoia.conversation.inference.exception.ModelNotSelectedException;
import com.nexoia.conversation.inference.intent.UserRequestIntentResolver;
import com.nexoia.provider.dto.ChatCompletionMessage;
import com.nexoia.provider.dto.ProviderModelResponse;
import com.nexoia.provider.exception.ProviderConfigurationNotFoundException;
import com.nexoia.provider.model.ProviderConfiguration;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.repository.ProviderConfigurationRepository;
import com.nexoia.provider.service.OllamaProviderService;
import com.nexoia.provider.service.ProviderEndpointGuard;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Chooses the effective execution mode and model before the transactional message reservation.
 * Provider discovery is intentionally outside the reservation transaction because it is a network
 * operation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRequestExecutionPlanner {

    private static final List<MessageStatus> INTENT_HISTORY =
            List.of(MessageStatus.COMPLETED, MessageStatus.CANCELLED);

    private final ConversationRepository conversations;
    private final ConversationMessageRepository messages;
    private final ProviderConfigurationRepository providers;
    private final ProviderEndpointGuard endpointGuard;
    private final OllamaProviderService ollama;

    public ModelRequestExecutionPlan plan(
            UUID userId,
            UUID conversationId,
            String content,
            ConversationMode requestedMode,
            boolean thinkingEnabled) {
        Conversation conversation = conversations.findByIdAndUserIdAndArchivedFalse(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
        if (!conversation.hasSelectedModel()) {
            throw new ModelNotSelectedException();
        }

        ProviderConfiguration provider = providers
                .findByIdAndUserId(conversation.getProviderConfigurationId(), userId)
                .orElseThrow(ProviderConfigurationNotFoundException::new);
        String objective = effectiveObjective(conversationId, content);
        boolean promote = requestedMode == ConversationMode.CHAT
                && conversation.getWorkspaceId() != null
                && UserRequestIntentResolver.requestsWorkspaceAction(objective);
        ConversationMode effectiveMode = promote ? ConversationMode.AGENT : requestedMode;
        String selectedModel = conversation.getSelectedModel();
        AgentModelSelection modelSelection = effectiveMode == ConversationMode.AGENT
                ? toolCapableModels(provider, selectedModel, thinkingEnabled)
                : new AgentModelSelection(selectedModel, null);
        String executionModel = modelSelection.primary();

        if (promote) {
            log.info(
                    "[NEXO-BACK][INFERENCE] Promoted Workspace action to Agent conversationId={}",
                    conversationId);
        }
        if (!executionModel.equals(selectedModel)) {
            log.info(
                    "[NEXO-BACK][INFERENCE] Selected request-local Agent executor conversationId={} selectedModel={} executionModel={}",
                    conversationId,
                    selectedModel,
                    executionModel);
        }
        return new ModelRequestExecutionPlan(
                effectiveMode,
                executionModel,
                modelSelection.fallback(),
                objective,
                promote,
                !executionModel.equals(selectedModel));
    }

    private String effectiveObjective(UUID conversationId, String content) {
        List<ChatCompletionMessage> history = new ArrayList<>(messages
                .findContextHistory(conversationId, INTENT_HISTORY)
                .stream()
                .filter(message -> message.getRole() == ConversationRole.USER)
                .map(message -> new ChatCompletionMessage("user", message.getContent()))
                .toList());
        history.add(new ChatCompletionMessage("user", content));
        return UserRequestIntentResolver.effectiveRequest(history);
    }

    private AgentModelSelection toolCapableModels(
            ProviderConfiguration provider, String selectedModel, boolean thinkingEnabled) {
        if (provider.getProviderType() != ProviderType.OLLAMA) {
            return new AgentModelSelection(selectedModel, null);
        }
        endpointGuard.verify(provider.getProviderType(), provider.getEndpoint());
        List<ProviderModelResponse> catalog = ollama.models(provider.getEndpoint());
        ProviderModelResponse selected = catalog.stream()
                .filter(model -> selectedModel.equals(model.name()))
                .findFirst()
                .orElse(null);
        List<ProviderModelResponse> candidates = catalog.stream()
                .filter(model -> Boolean.TRUE.equals(model.toolCallingSupported()))
                .toList();
        if (candidates.isEmpty()) {
            throw new AgentCapableModelUnavailableException();
        }
        ProviderModelResponse providerDefault = candidates.stream()
                .filter(model -> Objects.equals(provider.getSelectedModel(), model.name()))
                .findFirst()
                .orElse(null);

        Long selectedSize = selected == null ? null : selected.size();
        Comparator<ProviderModelResponse> preference = Comparator
                .comparing((ProviderModelResponse model) -> providerDefault == null
                        || !providerDefault.name().equals(model.name()))
                .thenComparing(model -> thinkingEnabled
                        && !Boolean.TRUE.equals(model.thinkingSupported()))
                .thenComparingLong(model -> sizeDistance(selectedSize, model.size()))
                .thenComparing(ProviderModelResponse::name);
        ProviderModelResponse primary = selected != null
                        && candidates.stream().anyMatch(model -> model.name().equals(selected.name()))
                ? selected
                : candidates.stream().min(preference).orElseThrow(AgentCapableModelUnavailableException::new);
        String fallback = candidates.stream()
                .filter(model -> !model.name().equals(primary.name()))
                .min(preference)
                .map(ProviderModelResponse::name)
                .orElse(null);
        return new AgentModelSelection(primary.name(), fallback);
    }

    private long sizeDistance(Long selectedSize, Long candidateSize) {
        if (Objects.isNull(selectedSize) || Objects.isNull(candidateSize)) {
            return Long.MAX_VALUE;
        }
        return Math.abs(selectedSize - candidateSize);
    }

    private record AgentModelSelection(String primary, String fallback) {}
}
