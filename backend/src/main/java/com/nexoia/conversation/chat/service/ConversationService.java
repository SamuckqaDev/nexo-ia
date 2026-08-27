package com.nexoia.conversation.chat.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditOutcome;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.conversation.chat.dto.AgentPlanResponse;
import com.nexoia.conversation.chat.dto.AgentPlanStepResponse;
import com.nexoia.conversation.chat.dto.ConversationMessageResponse;
import com.nexoia.conversation.chat.dto.ConversationResponse;
import com.nexoia.conversation.chat.dto.CreateConversationRequest;
import com.nexoia.conversation.chat.dto.RenameConversationRequest;
import com.nexoia.conversation.chat.dto.ToolExecutionResponse;
import com.nexoia.conversation.chat.dto.UpdateConversationKnowledgeRequest;
import com.nexoia.conversation.chat.dto.UpdateConversationModelRequest;
import com.nexoia.conversation.chat.dto.UpdateConversationWorkspaceRequest;
import com.nexoia.conversation.chat.exception.ConversationNotFoundException;
import com.nexoia.conversation.chat.model.Conversation;
import com.nexoia.conversation.chat.model.ConversationMessage;
import com.nexoia.conversation.chat.model.ConversationMode;
import com.nexoia.conversation.chat.model.ConversationRole;
import com.nexoia.conversation.chat.model.MessageStatus;
import com.nexoia.conversation.chat.repository.ConversationMessageRepository;
import com.nexoia.conversation.chat.repository.ConversationRepository;
import com.nexoia.conversation.inference.config.ConversationContextProperties;
import com.nexoia.conversation.inference.model.AgentPlanRecord;
import com.nexoia.conversation.inference.model.ToolExecutionRecord;
import com.nexoia.conversation.inference.repository.AgentPlanRepository;
import com.nexoia.conversation.inference.repository.ToolExecutionRepository;
import com.nexoia.provider.exception.ProviderConfigurationNotFoundException;
import com.nexoia.provider.repository.ProviderConfigurationRepository;
import com.nexoia.workspace.model.WorkspaceBinding;
import com.nexoia.workspace.service.WorkspaceBindingService;
import com.nexoia.workspace.service.WorkspaceService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ConversationService {

    private final ConversationRepository conversations;
    private final ConversationMessageRepository messages;
    private final ToolExecutionRepository toolExecutions;
    private final AgentPlanRepository agentPlans;
    private final ProviderConfigurationRepository providers;
    private final AuditService audit;
    private final ConversationContextProperties contextProperties;
    private final ConversationKnowledgeService knowledge;
    private final WorkspaceService workspaceService;
    private final WorkspaceBindingService workspaceBindings;

    public ConversationService(
            ConversationRepository conversations,
            ConversationMessageRepository messages,
            ToolExecutionRepository toolExecutions,
            AgentPlanRepository agentPlans,
            ProviderConfigurationRepository providers,
            AuditService audit,
            ConversationContextProperties contextProperties,
            ConversationKnowledgeService knowledge,
            WorkspaceService workspaceService) {
        this(conversations, messages, toolExecutions, agentPlans, providers, audit, contextProperties,
                knowledge, workspaceService, null);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(UUID userId) {
        List<Conversation> owned = conversations.findAllByUserIdAndArchivedFalseOrderByUpdatedAtDesc(userId);
        Map<UUID, List<UUID>> selected = knowledge.selectionIdsByConversation(
                owned.stream().map(Conversation::getId).toList());
        return owned.stream()
                .map(conversation -> conversationResponse(
                        conversation, selected.getOrDefault(conversation.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ConversationResponse create(UUID userId, CreateConversationRequest request) {
        Conversation conversation = conversations.save(Conversation.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .title(request.title().trim())
                .archived(false)
                .build());
        audit.record(RecordAuditCommand.success(
                AuditAction.CONVERSATION_CREATED, userId, null,
                AuditTargetType.CONVERSATION, conversation.getId()));

        return conversationResponse(conversation, List.of());
    }

    @Transactional(readOnly = true)
    public List<ConversationMessageResponse> messages(UUID userId, UUID conversationId) {
        readableConversation(userId, conversationId);

        List<ConversationMessage> conversationMessages =
                messages.findAllByConversationIdOrderBySequenceNumberAsc(conversationId);
        Map<UUID, List<ToolExecutionRecord>> executionsByMessage = conversationMessages.isEmpty()
                ? Map.of()
                : toolExecutions.findAllByAssistantMessageIdInOrderByStartedAtAsc(
                                conversationMessages.stream().map(ConversationMessage::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(ToolExecutionRecord::getAssistantMessageId));
        Map<UUID, AgentPlanRecord> plansByMessage = conversationMessages.isEmpty()
                ? Map.of()
                : agentPlans.findAllByAssistantMessageIdIn(
                                conversationMessages.stream().map(ConversationMessage::getId).toList())
                        .stream()
                        .collect(Collectors.toMap(AgentPlanRecord::getAssistantMessageId, plan -> plan));
        return conversationMessages.stream()
                .map(message -> messageResponse(
                        message,
                        executionsByMessage.getOrDefault(message.getId(), List.of()),
                        plansByMessage.get(message.getId())))
                .toList();
    }

    @Transactional
    public ConversationMessageResponse addUserMessage(UUID userId, UUID conversationId, String content) {
        writableConversation(userId, conversationId);
        int sequenceNumber = messages.findHighestSequenceNumber(conversationId) + 1;

        ConversationMessage message = messages.save(ConversationMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .sequenceNumber(sequenceNumber)
                .role(ConversationRole.USER)
                .status(MessageStatus.COMPLETED)
                .content(content.trim())
                .mode(ConversationMode.CHAT)
                .build());

        return messageResponse(message);
    }

    @Transactional
    public ConversationResponse rename(UUID userId, UUID conversationId, RenameConversationRequest request) {
        Conversation conversation = writableConversation(userId, conversationId);
        conversation.rename(request.title().trim());
        audit.record(RecordAuditCommand.success(
                AuditAction.CONVERSATION_RENAMED, userId, null,
                AuditTargetType.CONVERSATION, conversationId));

        return conversationResponse(conversation, knowledge.selectionIds(conversationId));
    }

    @Transactional
    public void archive(UUID userId, UUID conversationId) {
        writableConversation(userId, conversationId).archive();
        audit.record(RecordAuditCommand.success(
                AuditAction.CONVERSATION_ARCHIVED, userId, null,
                AuditTargetType.CONVERSATION, conversationId));
    }

    @Transactional
    public ConversationResponse selectModel(
            UUID userId, UUID conversationId, UpdateConversationModelRequest request) {
        Conversation conversation = writableConversation(userId, conversationId);
        providers.findByIdAndUserId(request.providerConfigurationId(), userId)
                .orElseThrow(ProviderConfigurationNotFoundException::new);
        conversation.selectModel(request.providerConfigurationId(), request.selectedModel().trim());
        audit.record(new RecordAuditCommand(
                AuditAction.CONVERSATION_MODEL_SELECTED, AuditOutcome.SUCCESS,
                userId, null, AuditTargetType.CONVERSATION, conversationId, null,
                request.selectedModel().trim()));

        return conversationResponse(conversation, knowledge.selectionIds(conversationId));
    }

    /**
     * Binds an owned Workspace to the conversation as durable agent scope, or clears it when the
     * request carries a null id. The Workspace is authorized against the caller here so later requests
     * can re-read the persisted selection without trusting any workspace id sent inside a message.
     */
    @Transactional
    public ConversationResponse selectWorkspace(
            UUID userId, UUID conversationId, UpdateConversationWorkspaceRequest request) {
        Conversation conversation = writableConversation(userId, conversationId);
        if (request.workspaceId() == null) {
            conversation.clearWorkspace();
        } else {
            workspaceService.ownedWorkspace(userId, request.workspaceId());
            UUID bindingId = request.workspaceBindingId();
            if (request.workspaceBindingId() != null) {
                if (workspaceBindings == null) {
                    throw new IllegalStateException("Workspace bindings are unavailable");
                }
                workspaceBindings.ownedBinding(userId, request.workspaceId(), request.workspaceBindingId());
            } else if (workspaceBindings != null) {
                bindingId = workspaceBindings.preferredAvailable(userId, request.workspaceId())
                        .map(WorkspaceBinding::getId)
                        .orElse(null);
            }
            conversation.attachWorkspace(request.workspaceId(), bindingId);
        }
        audit.record(RecordAuditCommand.success(
                AuditAction.CONVERSATION_WORKSPACE_SELECTED, userId, null,
                AuditTargetType.CONVERSATION, conversationId));

        return conversationResponse(conversation, knowledge.selectionIds(conversationId));
    }

    @Transactional
    public ConversationResponse selectKnowledge(
            UUID userId, UUID conversationId, UpdateConversationKnowledgeRequest request) {
        List<UUID> selected = knowledge.replace(userId, conversationId, request);
        Conversation conversation = conversations.findByIdAndUserIdAndArchivedFalse(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
        return conversationResponse(conversation, selected);
    }

    private Conversation readableConversation(UUID userId, UUID conversationId) {
        return conversations.findByIdAndUserIdAndArchivedFalse(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
    }

    private Conversation writableConversation(UUID userId, UUID conversationId) {
        return conversations.findOwnedForUpdate(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
    }

    private ConversationResponse conversationResponse(
            Conversation value, List<UUID> knowledgeVaultIds) {
        return new ConversationResponse(
                value.getId(),
                value.getTitle(),
                value.getProviderConfigurationId(),
                value.getSelectedModel(),
                knowledgeVaultIds,
                value.getWorkspaceId(),
                value.getWorkspaceBindingId(),
                value.getCreatedAt(),
                value.getUpdatedAt());
    }

    private ConversationMessageResponse messageResponse(ConversationMessage value) {
        return messageResponse(value, List.of(), null);
    }

    private ConversationMessageResponse messageResponse(
            ConversationMessage value,
            List<ToolExecutionRecord> executions,
            AgentPlanRecord plan) {
        return new ConversationMessageResponse(
                value.getId(),
                value.getRole(),
                value.getStatus(),
                value.getContent(),
                value.getModel(),
                value.getInputTokens(),
                value.getOutputTokens(),
                totalTokens(value),
                value.getInputTokens(),
                value.getRole() == ConversationRole.ASSISTANT
                        ? contextProperties.tokenBudget()
                        : null,
                value.getTokenSource(),
                value.getLatencyMs(),
                value.getProcessingLocation(),
                value.getFailureCode(),
                value.getMode() == null ? ConversationMode.CHAT : value.getMode(),
                value.getAgentState(),
                value.getCreatedAt(),
                value.getCompletedAt(),
                value.getCitations(),
                executions.stream().map(this::toolExecutionResponse).toList(),
                agentPlanResponse(plan));
    }

    private AgentPlanResponse agentPlanResponse(AgentPlanRecord plan) {
        if (plan == null) {
            return null;
        }
        return new AgentPlanResponse(
                plan.getRevision(),
                plan.getExplanation(),
                plan.getSteps().stream()
                        .map(step -> new AgentPlanStepResponse(
                                step.step(), step.description(), step.status()))
                        .toList(),
                plan.getUpdatedAt());
    }

    private ToolExecutionResponse toolExecutionResponse(ToolExecutionRecord execution) {
        return new ToolExecutionResponse(
                execution.getId(),
                execution.getToolName(),
                execution.getStatus(),
                execution.getDurationMs(),
                execution.getCitations() == null ? List.of() : execution.getCitations(),
                execution.getStartedAt(),
                execution.getCompletedAt());
    }

    private Long totalTokens(ConversationMessage message) {
        if (message.getInputTokens() == null && message.getOutputTokens() == null) {
            return null;
        }

        return (message.getInputTokens() == null ? 0L : message.getInputTokens().longValue())
                + (message.getOutputTokens() == null ? 0L : message.getOutputTokens().longValue());
    }
}
