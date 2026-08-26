package com.nexoia.conversation.inference.service;

import com.nexoia.auth.user.exception.UserNotFoundException;
import com.nexoia.auth.user.model.UserAccount;
import com.nexoia.auth.user.repository.UserAccountRepository;
import com.nexoia.conversation.chat.exception.ConversationBusyException;
import com.nexoia.conversation.chat.exception.ConversationNotFoundException;
import com.nexoia.conversation.chat.model.Conversation;
import com.nexoia.conversation.chat.model.ConversationMessage;
import com.nexoia.conversation.chat.model.ConversationMode;
import com.nexoia.conversation.chat.model.ConversationRole;
import com.nexoia.conversation.chat.model.MessageStatus;
import com.nexoia.conversation.chat.repository.ConversationMessageRepository;
import com.nexoia.conversation.chat.repository.ConversationRepository;
import com.nexoia.conversation.chat.service.ConversationKnowledgeService;
import com.nexoia.conversation.inference.context.CapabilityManifest;
import com.nexoia.conversation.inference.context.ContextSourceSummary;
import com.nexoia.conversation.inference.context.KnowledgeCapability;
import com.nexoia.conversation.inference.context.KnowledgeSearchStatus;
import com.nexoia.conversation.inference.context.ModelContextEnvelope;
import com.nexoia.conversation.inference.context.PermissionCapability;
import com.nexoia.conversation.inference.context.ResolvedKnowledgeContext;
import com.nexoia.conversation.inference.context.SkillCapability;
import com.nexoia.conversation.inference.context.ToolCapability;
import com.nexoia.conversation.inference.context.WorkspaceCapability;
import com.nexoia.conversation.inference.dto.ModelRequestReservation;
import com.nexoia.conversation.inference.exception.ModelNotSelectedException;
import com.nexoia.conversation.inference.exception.ModelRequestNotFoundException;
import com.nexoia.conversation.inference.model.AgentPlanRecord;
import com.nexoia.conversation.inference.model.AgentPlanStep;
import com.nexoia.conversation.inference.model.AgentState;
import com.nexoia.conversation.inference.model.ToolExecutionRecord;
import com.nexoia.conversation.inference.repository.AgentPlanRepository;
import com.nexoia.conversation.inference.repository.ToolExecutionRepository;
import com.nexoia.conversation.inference.tool.AgentPlanToolFactory;
import com.nexoia.knowledge.embedding.exception.EmbeddingProviderUnavailableException;
import com.nexoia.knowledge.ingestion.tool.KnowledgeWriteToolFactory;
import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.knowledge.retrieval.dto.RetrievalQuery;
import com.nexoia.knowledge.retrieval.dto.RetrievalResult;
import com.nexoia.knowledge.retrieval.service.RetrievalService;
import com.nexoia.knowledge.retrieval.tool.KnowledgeSearchToolFactory;
import com.nexoia.knowledge.vault.model.KnowledgeVault;
import com.nexoia.mcp.connection.service.McpConnectionService;
import com.nexoia.mcp.runtime.dto.McpRuntimeConnection;
import com.nexoia.memory.personal.service.PersonalMemoryService;
import com.nexoia.memory.personal.tool.RememberToolFactory;
import com.nexoia.permission.dto.ResolvedPermissions;
import com.nexoia.permission.model.BuiltInProfiles;
import com.nexoia.permission.model.CapabilityFamily;
import com.nexoia.permission.model.ContentStance;
import com.nexoia.permission.model.PermissionProfile;
import com.nexoia.permission.model.ProfileKey;
import com.nexoia.permission.model.UnlockLevel;
import com.nexoia.permission.service.PermissionEngine;
import com.nexoia.provider.dto.AgentPlanToolScope;
import com.nexoia.provider.dto.AgentPlanUpdate;
import com.nexoia.provider.dto.AgentPlanUpdateObserver;
import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.dto.KnowledgeToolScope;
import com.nexoia.provider.dto.KnowledgeWriteToolScope;
import com.nexoia.provider.dto.McpToolScope;
import com.nexoia.provider.dto.MemoryToolScope;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.dto.ToolExecutionObserver;
import com.nexoia.provider.dto.ToolExecutionStarted;
import com.nexoia.provider.dto.ToolExecutionStatus;
import com.nexoia.provider.dto.WorkspaceToolScope;
import com.nexoia.provider.exception.ProviderConfigurationNotFoundException;
import com.nexoia.provider.model.ProcessingLocation;
import com.nexoia.provider.model.ProviderConfiguration;
import com.nexoia.provider.repository.ProviderConfigurationRepository;
import com.nexoia.provider.service.ProviderEndpointGuard;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceStatus;
import com.nexoia.workspace.service.WorkspaceAccessService;
import com.nexoia.workspace.tool.WorkspaceReadToolFactory;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The transactional half of a model request.
 *
 * <p>It is a separate bean on purpose. Each method here must run in its own short transaction, and a
 * self-invoked {@code @Transactional} method inside {@link ModelRequestService} would bypass the
 * Spring proxy and silently run without one.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRequestStore {

    private static final Set<MessageStatus> IN_FLIGHT =
            Set.of(MessageStatus.QUEUED, MessageStatus.STREAMING, MessageStatus.CANCELLING);

    private final ConversationRepository conversations;
    private final ConversationMessageRepository messages;
    private final ToolExecutionRepository toolExecutions;
    private final AgentPlanRepository agentPlans;
    private final ProviderConfigurationRepository providers;
    private final UserAccountRepository users;
    private final ConversationKnowledgeService conversationKnowledge;
    private final ConversationContextAssembler contextAssembler;
    private final ProviderEndpointGuard endpointGuard;
    private final RetrievalService retrievalService;
    private final McpConnectionService mcpConnections;
    private final PersonalMemoryService personalMemories;
    private final PermissionEngine permissionEngine;
    private final WorkspaceAccessService workspaceAccess;
    private final Clock clock;

    /**
     * Reserves without selecting any Knowledge Vault — equivalent to {@code knowledgeVaultIds =
     * List.of()}.
     */
    public ModelRequestReservation reserve(
            UUID userId, UUID conversationId, String content, boolean thinkingEnabled) {
        return reserve(
                userId, conversationId, content, thinkingEnabled, List.of(), ConversationMode.CHAT);
    }

    /**
     * Appends the user message and reserves the assistant message, returning the values the
     * streaming stage needs. Holds the conversation write lock for the duration of this transaction
     * only.
     *
     * <p>The request-level Vault list is retained temporarily for wire compatibility and deliberately
     * ignored. Only the durable, authorized conversation selection is trusted.
     *
     * <p>Retrieval failure (an unavailable embedding provider) never fails the request — chat must
     * keep working with zero citations, per D-026. Only an explicit isolation boundary (an
     * unauthorized or archived vault) is allowed to silently produce no citations; any other
     * retrieval defect is a bug, not a reason to degrade silently, so only
     * {@link EmbeddingProviderUnavailableException} is caught here.
     */
    @Transactional
    public ModelRequestReservation reserve(
            UUID userId, UUID conversationId, String content, boolean thinkingEnabled,
            List<UUID> ignoredKnowledgeVaultIds) {
        return reserve(
                userId, conversationId, content, thinkingEnabled,
                ignoredKnowledgeVaultIds, ConversationMode.CHAT);
    }

    @Transactional
    public ModelRequestReservation reserve(
            UUID userId,
            UUID conversationId,
            String content,
            boolean thinkingEnabled,
            List<UUID> ignoredKnowledgeVaultIds,
            ConversationMode mode) {
        Conversation conversation = conversations.findOwnedForUpdate(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);

        if (!conversation.hasSelectedModel()) {
            throw new ModelNotSelectedException();
        }

        ProviderConfiguration provider = providers
                .findByIdAndUserId(conversation.getProviderConfigurationId(), userId)
                .orElseThrow(ProviderConfigurationNotFoundException::new);
        ProcessingLocation processingLocation =
                endpointGuard.verify(provider.getProviderType(), provider.getEndpoint());

        UUID correlationId = UUID.randomUUID();
        int sequenceNumber = messages.findHighestSequenceNumber(conversationId);

        ConversationMessage userMessage = messages.save(ConversationMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .sequenceNumber(sequenceNumber + 1)
                .role(ConversationRole.USER)
                .status(MessageStatus.COMPLETED)
                .content(content.trim())
                .correlationId(correlationId)
                .mode(mode)
                .build());

        ConversationMessage assistantMessage = ConversationMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .sequenceNumber(sequenceNumber + 2)
                .role(ConversationRole.ASSISTANT)
                .status(MessageStatus.QUEUED)
                .content("")
                .providerConfigurationId(provider.getId())
                .model(conversation.getSelectedModel())
                .processingLocation(processingLocation)
                .correlationId(correlationId)
                .mode(mode)
                .agentState(mode == ConversationMode.AGENT ? AgentState.PLANNING : null)
                .build();

        try {
            // Flushed here so the partial unique index rejects a second active request while the
            // caller can still be given a normal conflict response.
            messages.saveAndFlush(assistantMessage);
        } catch (DataIntegrityViolationException exception) {
            throw new ConversationBusyException();
        }

        UserAccount account = users.findById(userId).orElseThrow(UserNotFoundException::new);
        String username = account.getUsername();
        ProfileKey assignedProfile = account.getAssignedProfile();
        List<KnowledgeVault> selectedVaults = conversationKnowledge.selectedVaults(userId, conversationId);
        List<UUID> selectedVaultIds = selectedVaults.stream().map(KnowledgeVault::getId).toList();
        KnowledgeVault writableVault = mode == ConversationMode.AGENT
                ? selectedVaults.stream().filter(KnowledgeVault::isWritable).findFirst().orElse(null)
                : null;
        ResolvedKnowledgeContext resolvedKnowledge = mode == ConversationMode.CHAT
                ? retrieve(userId, selectedVaultIds, content)
                : selectedVaultIds.isEmpty()
                        ? ResolvedKnowledgeContext.notRequested()
                        : ResolvedKnowledgeContext.availableOnDemand();
        List<CitationResponse> citations = resolvedKnowledge.citations();
        List<McpRuntimeConnection> enabledMcpConnections = mode == ConversationMode.AGENT
                ? mcpConnections.enabledRuntimeConnections(userId)
                : List.of();
        Workspace selectedWorkspace = conversation.getWorkspaceId() == null
                ? null
                : workspaceAccess.accessibleWorkspace(userId, conversation.getWorkspaceId());
        boolean workspaceAvailable = selectedWorkspace != null
                && workspaceAccess.lightStatus(selectedWorkspace) == WorkspaceStatus.AVAILABLE;
        RequestPermission requestPermission = resolvePermission(
                mode, enabledMcpConnections, writableVault != null, workspaceAvailable, assignedProfile);
        boolean workspaceReadAllowed = mode == ConversationMode.AGENT
                && workspaceAvailable
                && requestPermission.resolved().isAllowed(CapabilityFamily.WORKSPACE_READ);

        return new ModelRequestReservation(
                userId,
                userMessage.getId(),
                assistantMessage.getId(),
                correlationId,
                new ChatCompletionCommand(
                        provider.getProviderType(),
                        provider.getEndpoint(),
                        conversation.getSelectedModel(),
                        contextAssembler.assemble(
                                conversationId, username, citations,
                                capabilityEnvelope(username, conversation.getSelectedModel(),
                                        processingLocation, selectedVaults, resolvedKnowledge,
                                        enabledMcpConnections, selectedWorkspace,
                                        workspaceReadAllowed, requestPermission, mode),
                                personalMemories.context(userId)),
                        thinkingEnabled,
                        mode,
                        mode == ConversationMode.AGENT
                                ? new KnowledgeToolScope(
                                        userId, assistantMessage.getId(), correlationId, selectedVaultIds)
                                : null,
                        mode == ConversationMode.AGENT
                                ? new AgentPlanToolScope(
                                        userId, assistantMessage.getId(), correlationId, content.trim())
                                : null,
                        mode == ConversationMode.AGENT
                                ? new MemoryToolScope(
                                        userId, conversationId, assistantMessage.getId(), correlationId)
                                : null,
                        mode == ConversationMode.AGENT && !enabledMcpConnections.isEmpty()
                                ? new McpToolScope(
                                        userId, assistantMessage.getId(), correlationId,
                                        enabledMcpConnections)
                                : null,
                        writableVault == null
                                ? null
                                : new KnowledgeWriteToolScope(
                                        userId, writableVault.getId(), writableVault.getName(),
                                        assistantMessage.getId(), correlationId),
                        workspaceReadAllowed
                                ? new WorkspaceToolScope(
                                        userId,
                                        conversationId,
                                        assistantMessage.getId(),
                                        correlationId,
                                        selectedWorkspace.getId(),
                                        selectedWorkspace.getName(),
                                        selectedWorkspace.getAccessMode(),
                                        true)
                                : null,
                        ToolExecutionObserver.NOOP,
                        AgentPlanUpdateObserver.NOOP),
                processingLocation,
                citations);
    }

    /**
     * Builds the truthful per-request capability envelope. The knowledge numbers are derived from
     * the deterministic retrieval that already ran, so the model cannot be told it searched or found
     * more than it did. Workspace tools are exposed only when the persisted conversation selection,
     * binding and Permission Engine all authorize server-side reads. Agent mode additionally exposes
     * only the user's enabled, explicitly selected MCP tools.
     */
    private ModelContextEnvelope capabilityEnvelope(
            String username, String model,
            ProcessingLocation processingLocation,
            List<KnowledgeVault> selectedVaults,
            ResolvedKnowledgeContext resolvedKnowledge,
            List<McpRuntimeConnection> enabledMcpConnections,
            Workspace selectedWorkspace,
            boolean workspaceReadAllowed,
            RequestPermission requestPermission,
            ConversationMode mode) {
        List<CitationResponse> citations = resolvedKnowledge.citations();
        KnowledgeCapability knowledge = new KnowledgeCapability(
                selectedVaults.stream().map(KnowledgeVault::getName).toList(),
                selectedVaults.size(),
                resolvedKnowledge.status(),
                citations.size(),
                citations.stream()
                        .map(c -> new ContextSourceSummary(c.vaultName(), c.sourceDisplayName(), c.chunkOrdinal()))
                        .toList());

        boolean hasWritableVault = mode == ConversationMode.AGENT
                && selectedVaults.stream().anyMatch(KnowledgeVault::isWritable);

        ToolCapability tools;
        if (mode != ConversationMode.AGENT) {
            tools = ToolCapability.none();
        } else {
            List<String> exposedTools = new ArrayList<>();
            exposedTools.add(AgentPlanToolFactory.TOOL_NAME);
            exposedTools.add(RememberToolFactory.TOOL_NAME);
            if (!selectedVaults.isEmpty()) {
                exposedTools.add(KnowledgeSearchToolFactory.TOOL_NAME);
            }
            if (hasWritableVault) {
                exposedTools.add(KnowledgeWriteToolFactory.TOOL_NAME);
            }
            if (workspaceReadAllowed) {
                exposedTools.addAll(List.of(
                        WorkspaceReadToolFactory.LIST_FILES,
                        WorkspaceReadToolFactory.READ_FILE,
                        WorkspaceReadToolFactory.SEARCH,
                        WorkspaceReadToolFactory.GIT_STATUS,
                        WorkspaceReadToolFactory.GIT_DIFF,
                        WorkspaceReadToolFactory.INSPECT_PROJECT));
            }
            enabledMcpConnections.stream()
                    .flatMap(connection -> connection.enabledTools().stream())
                    .map(tool -> tool.exposedName())
                    .forEach(exposedTools::add);
            tools = new ToolCapability(List.copyOf(exposedTools));
        }

        return new ModelContextEnvelope(username, mode.name().toLowerCase(),
                new CapabilityManifest(model, processingLocation, requestPermission.capability(), knowledge,
                        selectedWorkspace == null
                                ? WorkspaceCapability.none()
                                : new WorkspaceCapability(
                                        true, selectedWorkspace.getName(), workspaceReadAllowed),
                        SkillCapability.none(), tools));
    }

    /**
     * Resolves the request's effective permission level and content matrix through the deterministic
     * {@link PermissionEngine}, so the envelope tells the model its capability boundary, the honest unlock
     * path, and the per-area content policy. The user's assigned profile governs the capability ceiling and
     * carries the content matrix; the conversation mode still clamps Chat to grounded (no tool loop). The
     * capability axis and the content axis stay independent.
     */
    private RequestPermission resolvePermission(
            ConversationMode mode,
            List<McpRuntimeConnection> enabledMcpConnections,
            boolean hasWritableVault,
            boolean hasAvailableWorkspace,
            ProfileKey assignedProfile) {
        boolean agent = mode == ConversationMode.AGENT;
        PermissionProfile profile = BuiltInProfiles.of(
                assignedProfile == null ? ProfileKey.RESEARCHER : assignedProfile);
        UnlockLevel modeCeiling = agent ? UnlockLevel.L5_OPERATOR : UnlockLevel.L1_GROUNDED;

        EnumSet<CapabilityFamily> authorizedTargets = EnumSet.noneOf(CapabilityFamily.class);
        if (!enabledMcpConnections.isEmpty()) {
            authorizedTargets.add(CapabilityFamily.EXTERNAL_READ);
        }
        if (hasWritableVault) {
            authorizedTargets.add(CapabilityFamily.KNOWLEDGE_WRITE);
        }
        if (hasAvailableWorkspace) {
            authorizedTargets.add(CapabilityFamily.WORKSPACE_READ);
        }

        ResolvedPermissions resolved = permissionEngine.resolve(
                profile, modeCeiling, agent, authorizedTargets, ContentStance.STANDARD);

        List<String> locked = resolved.locked().stream()
                .filter(family -> !family.prohibited())
                .map(CapabilityFamily::label)
                .toList();
        return new RequestPermission(
                resolved,
                new PermissionCapability(
                        profile.name(), resolved.effectiveLevel(), profile.contentMatrix(), locked));
    }

    private record RequestPermission(
            ResolvedPermissions resolved,
            PermissionCapability capability) {}

    private ResolvedKnowledgeContext retrieve(
            UUID userId, List<UUID> knowledgeVaultIds, String content) {
        if (knowledgeVaultIds.isEmpty()) {
            return ResolvedKnowledgeContext.notRequested();
        }

        try {
            RetrievalResult result = retrievalService.retrieve(userId, new RetrievalQuery(knowledgeVaultIds, content));
            return new ResolvedKnowledgeContext(result.citations(), KnowledgeSearchStatus.COMPLETED);
        } catch (EmbeddingProviderUnavailableException exception) {
            log.warn("[NEXO-BACK][KNOWLEDGE] Embedding provider unavailable; continuing without citations userId={}",
                    userId);
            return ResolvedKnowledgeContext.unavailable();
        }
    }

    @Transactional
    public void markStreaming(UUID messageId) {
        messages.findById(messageId).ifPresent(ConversationMessage::markStreaming);
    }

    @Transactional
    public void markVerifying(UUID messageId) {
        messages.findById(messageId).ifPresent(ConversationMessage::markVerifying);
    }

    @Transactional
    public void recordToolStarted(
            UUID messageId, UUID correlationId, ToolExecutionStarted event) {
        toolExecutions.save(ToolExecutionRecord.builder()
                .id(event.executionId())
                .assistantMessageId(messageId)
                .correlationId(correlationId)
                .toolName(event.toolName())
                .argumentsDigest(event.argumentsDigest())
                .status(ToolExecutionStatus.RUNNING)
                .startedAt(event.startedAt())
                .build());
    }

    @Transactional
    public void recordToolCompleted(ToolExecutionEvidence event) {
        toolExecutions.findById(event.executionId())
                .ifPresent(record -> record.complete(event));
    }

    @Transactional
    public void recordPlanUpdated(UUID messageId, AgentPlanUpdate update) {
        AgentPlanRecord plan = agentPlans.findByAssistantMessageId(messageId)
                .orElseGet(() -> AgentPlanRecord.builder()
                        .id(UUID.randomUUID())
                        .assistantMessageId(messageId)
                        .revision(update.revision())
                        .explanation(update.explanation())
                        .steps(update.steps().stream()
                                .map(step -> new AgentPlanStep(
                                        step.step(), step.description(), step.status()))
                                .toList())
                        .createdAt(update.updatedAt())
                        .updatedAt(update.updatedAt())
                        .build());
        if (plan.getRevision() < update.revision()) {
            plan.update(update);
        }
        agentPlans.save(plan);
    }

    @Transactional
    public Instant recordCompletion(
            UUID messageId, ChatCompletionOutcome outcome, long latencyMs, List<CitationResponse> citations) {
        Instant completedAt = clock.instant();
        messages.findById(messageId).ifPresent(message -> message.complete(
                outcome.content(),
                outcome.inputTokens(),
                outcome.outputTokens(),
                outcome.tokenSource(),
                latencyMs,
                completedAt,
                citations));

        return completedAt;
    }

    @Transactional
    public Instant recordCancellation(UUID messageId, String partialContent, long latencyMs) {
        Instant cancelledAt = clock.instant();
        messages.findById(messageId)
                .ifPresent(message -> message.cancel(partialContent, latencyMs, cancelledAt));

        return cancelledAt;
    }

    @Transactional
    public void recordFailure(UUID messageId, String failureCode, long latencyMs) {
        messages.findById(messageId).ifPresent(message ->
                message.fail(failureCode, message.getContent(), latencyMs, clock.instant()));
    }

    /**
     * Moves a running request to CANCELLING. The streaming stage writes the terminal state once the
     * reading loop stops.
     */
    @Transactional
    public void markCancelling(UUID userId, UUID conversationId, UUID messageId) {
        conversations.findByIdAndUserIdAndArchivedFalse(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
        ConversationMessage message = messages.findByIdAndConversationId(messageId, conversationId)
                .orElseThrow(ModelRequestNotFoundException::new);

        if (message.getStatus().isTerminal()) {
            throw new ModelRequestNotFoundException();
        }

        message.markCancelling();
    }

    @Transactional
    public int failInFlightRequests(String failureCode) {
        List<ConversationMessage> inFlight = messages.findAllByStatusIn(IN_FLIGHT);
        Instant failedAt = clock.instant();
        inFlight.forEach(message -> message.fail(failureCode, message.getContent(), 0L, failedAt));

        return inFlight.size();
    }
}
