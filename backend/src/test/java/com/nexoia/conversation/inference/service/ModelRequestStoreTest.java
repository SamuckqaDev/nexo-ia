package com.nexoia.conversation.inference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.auth.user.model.UserAccount;
import com.nexoia.auth.user.repository.UserAccountRepository;
import com.nexoia.conversation.chat.model.Conversation;
import com.nexoia.conversation.chat.model.ConversationMessage;
import com.nexoia.conversation.chat.model.ConversationMode;
import com.nexoia.conversation.chat.repository.ConversationMessageRepository;
import com.nexoia.conversation.chat.repository.ConversationRepository;
import com.nexoia.conversation.chat.service.ConversationKnowledgeService;
import com.nexoia.conversation.inference.context.KnowledgeSearchStatus;
import com.nexoia.conversation.inference.context.ModelContextEnvelope;
import com.nexoia.conversation.inference.dto.ModelRequestReservation;
import com.nexoia.conversation.inference.repository.AgentPlanRepository;
import com.nexoia.conversation.inference.repository.ToolExecutionRepository;
import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.knowledge.retrieval.dto.RetrievalQuery;
import com.nexoia.knowledge.retrieval.dto.RetrievalResult;
import com.nexoia.knowledge.retrieval.service.RetrievalService;
import com.nexoia.knowledge.vault.model.KnowledgeVault;
import com.nexoia.knowledge.vault.model.VaultScope;
import com.nexoia.mcp.connection.model.McpConnectionKind;
import com.nexoia.mcp.connection.model.McpTransportType;
import com.nexoia.mcp.connection.service.McpConnectionService;
import com.nexoia.mcp.runtime.dto.McpRuntimeConnection;
import com.nexoia.mcp.runtime.dto.McpRuntimeTool;
import com.nexoia.memory.personal.service.PersonalMemoryService;
import com.nexoia.permission.model.ProfileKey;
import com.nexoia.permission.service.PermissionEngine;
import com.nexoia.provider.dto.ChatCompletionMessage;
import com.nexoia.provider.model.ProcessingLocation;
import com.nexoia.provider.model.ProviderConfiguration;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.repository.ProviderConfigurationRepository;
import com.nexoia.provider.service.ProviderEndpointGuard;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceAccessMode;
import com.nexoia.workspace.model.WorkspaceStatus;
import com.nexoia.workspace.model.WorkspaceStorageType;
import com.nexoia.workspace.service.WorkspaceAccessService;
import com.nexoia.workspace.tool.WorkspaceReadToolFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModelRequestStoreTest {

    @Mock private ConversationRepository conversations;
    @Mock private ConversationMessageRepository messages;
    @Mock private ToolExecutionRepository toolExecutions;
    @Mock private AgentPlanRepository agentPlans;
    @Mock private ProviderConfigurationRepository providers;
    @Mock private UserAccountRepository users;
    @Mock private ConversationKnowledgeService conversationKnowledge;
    @Mock private ConversationContextAssembler contextAssembler;
    @Mock private ProviderEndpointGuard endpointGuard;
    @Mock private RetrievalService retrieval;
    @Mock private McpConnectionService mcpConnections;
    @Mock private PersonalMemoryService personalMemories;
    @Mock private WorkspaceAccessService workspaceAccess;

    private ModelRequestStore store;
    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        store = new ModelRequestStore(
                conversations,
                messages,
                toolExecutions,
                agentPlans,
                providers,
                users,
                conversationKnowledge,
                contextAssembler,
                endpointGuard,
                retrieval,
                mcpConnections,
                personalMemories,
                new PermissionEngine(),
                workspaceAccess,
                Clock.fixed(Instant.parse("2026-08-21T12:00:00Z"), ZoneOffset.UTC));
        when(conversations.findOwnedForUpdate(conversationId, userId))
                .thenReturn(Optional.of(Conversation.builder()
                        .id(conversationId)
                        .userId(userId)
                        .title("Chat")
                        .providerConfigurationId(providerId)
                        .selectedModel("qwen3:8b")
                        .build()));
        when(providers.findByIdAndUserId(providerId, userId)).thenReturn(Optional.of(
                ProviderConfiguration.builder()
                        .id(providerId)
                        .userId(userId)
                        .providerType(ProviderType.OLLAMA)
                        .endpoint("http://127.0.0.1:11434")
                        .displayName("Local")
                        .enabled(true)
                        .build()));
        when(endpointGuard.verify(ProviderType.OLLAMA, "http://127.0.0.1:11434"))
                .thenReturn(ProcessingLocation.LOCAL);
        when(users.findById(userId)).thenReturn(Optional.of(
                UserAccount.builder().id(userId).username("owner").build()));
        when(messages.save(any(ConversationMessage.class))).thenAnswer(call -> call.getArgument(0));
        when(messages.saveAndFlush(any(ConversationMessage.class))).thenAnswer(call -> call.getArgument(0));
        when(contextAssembler.assemble(any(), any(), any(), any(), any())).thenReturn(
                List.of(new ChatCompletionMessage("user", "question")));
    }

    @Test
    void ignoresTransientClientVaultIdsAndRetrievesOnlyTheDurableAuthorizedSelection() {
        UUID authorizedVaultId = UUID.randomUUID();
        UUID forgedClientVaultId = UUID.randomUUID();
        KnowledgeVault vault = KnowledgeVault.builder()
                .id(authorizedVaultId)
                .ownerId(userId)
                .name("Nexo KB")
                .scope(VaultScope.PERSONAL)
                .build();
        CitationResponse citation = new CitationResponse(
                "Nexo KB", "Principles", 1, "Truthful", 0.9);
        when(conversationKnowledge.selectedVaults(userId, conversationId)).thenReturn(List.of(vault));
        when(retrieval.retrieve(any(), any())).thenReturn(new RetrievalResult(List.of(citation)));

        ModelRequestReservation reservation = store.reserve(
                userId,
                conversationId,
                "question",
                false,
                List.of(forgedClientVaultId),
                ConversationMode.CHAT);

        ArgumentCaptor<RetrievalQuery> query = ArgumentCaptor.forClass(RetrievalQuery.class);
        verify(retrieval).retrieve(eq(userId), query.capture());
        assertThat(query.getValue().vaultIds()).containsExactly(authorizedVaultId);
        assertThat(reservation.command().knowledgeToolScope()).isNull();
        assertThat(reservation.citations()).containsExactly(citation);
    }

    @Test
    void agentModeExposesAuthorizedKnowledgePlanAndOwnedMcpToolsWithoutPreRetrieval() {
        KnowledgeVault vault = KnowledgeVault.builder()
                .id(UUID.randomUUID())
                .ownerId(userId)
                .name("Nexo KB")
                .scope(VaultScope.PERSONAL)
                .build();
        when(conversationKnowledge.selectedVaults(userId, conversationId)).thenReturn(List.of(vault));
        McpRuntimeConnection mcp = new McpRuntimeConnection(
                UUID.randomUUID(), "Fetch", McpConnectionKind.DOCKER_CATALOG,
                McpTransportType.DOCKER_GATEWAY, "fetch", null,
                List.of(new McpRuntimeTool("fetch", "mcp_12345678_fetch")));
        when(mcpConnections.enabledRuntimeConnections(userId)).thenReturn(List.of(mcp));

        ModelRequestReservation reservation = store.reserve(
                userId,
                conversationId,
                "investigate",
                false,
                List.of(),
                ConversationMode.AGENT);

        verify(retrieval, never()).retrieve(any(), any());
        assertThat(reservation.command().knowledgeToolScope().authorizedVaultIds())
                .containsExactly(vault.getId());
        ArgumentCaptor<ModelContextEnvelope> envelope = ArgumentCaptor.forClass(ModelContextEnvelope.class);
        verify(contextAssembler).assemble(
                eq(conversationId), eq("owner"), eq(List.of()), envelope.capture(), eq(List.of()));
        assertThat(envelope.getValue().conversationMode()).isEqualTo("agent");
        assertThat(envelope.getValue().manifest().knowledge().searchStatus())
                .isEqualTo(KnowledgeSearchStatus.AVAILABLE_ON_DEMAND);
        assertThat(envelope.getValue().manifest().tools().exposedToolNames())
                .containsExactly("update_plan", "remember", "search_knowledge", "mcp_12345678_fetch");
        assertThat(reservation.command().agentPlanToolScope()).isNotNull();
        assertThat(reservation.command().memoryToolScope()).isNotNull();
        assertThat(reservation.command().mcpToolScope().connections()).containsExactly(mcp);
    }

    @Test
    void agentModeAttachesWorkspaceToolsOnlyFromThePersistedAuthorizedSelection() {
        UUID workspaceId = UUID.randomUUID();
        when(conversations.findOwnedForUpdate(conversationId, userId))
                .thenReturn(Optional.of(Conversation.builder()
                        .id(conversationId)
                        .userId(userId)
                        .title("Chat")
                        .providerConfigurationId(providerId)
                        .selectedModel("qwen3:8b")
                        .workspaceId(workspaceId)
                        .build()));
        when(users.findById(userId)).thenReturn(Optional.of(
                UserAccount.builder()
                        .id(userId)
                        .username("owner")
                        .assignedProfile(ProfileKey.OPERATOR)
                        .build()));
        Workspace workspace = Workspace.builder()
                .id(workspaceId)
                .ownerId(userId)
                .name("Nexo")
                .storageType(WorkspaceStorageType.MOUNTED)
                .accessMode(WorkspaceAccessMode.READ_ONLY)
                .relativePath("nexo")
                .build();
        when(workspaceAccess.accessibleWorkspace(userId, workspaceId)).thenReturn(workspace);
        when(workspaceAccess.lightStatus(workspace)).thenReturn(WorkspaceStatus.AVAILABLE);

        ModelRequestReservation reservation = store.reserve(
                userId, conversationId, "inspect the project", false, List.of(), ConversationMode.AGENT);

        assertThat(reservation.command().workspaceToolScope().workspaceId()).isEqualTo(workspaceId);
        ArgumentCaptor<ModelContextEnvelope> envelope = ArgumentCaptor.forClass(ModelContextEnvelope.class);
        verify(contextAssembler).assemble(
                eq(conversationId), eq("owner"), eq(List.of()), envelope.capture(), eq(List.of()));
        assertThat(envelope.getValue().manifest().workspace().serverSideAccess()).isTrue();
        assertThat(envelope.getValue().manifest().tools().exposedToolNames())
                .contains(
                        WorkspaceReadToolFactory.LIST_FILES,
                        WorkspaceReadToolFactory.READ_FILE,
                        WorkspaceReadToolFactory.SEARCH,
                        WorkspaceReadToolFactory.GIT_STATUS,
                        WorkspaceReadToolFactory.GIT_DIFF,
                        WorkspaceReadToolFactory.INSPECT_PROJECT);
    }
}
