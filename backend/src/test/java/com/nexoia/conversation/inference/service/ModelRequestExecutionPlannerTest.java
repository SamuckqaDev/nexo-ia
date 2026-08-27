package com.nexoia.conversation.inference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.nexoia.conversation.chat.model.Conversation;
import com.nexoia.conversation.chat.model.ConversationMessage;
import com.nexoia.conversation.chat.model.ConversationMode;
import com.nexoia.conversation.chat.model.ConversationRole;
import com.nexoia.conversation.chat.repository.ConversationMessageRepository;
import com.nexoia.conversation.chat.repository.ConversationRepository;
import com.nexoia.conversation.inference.dto.ModelRequestExecutionPlan;
import com.nexoia.conversation.inference.exception.AgentCapableModelUnavailableException;
import com.nexoia.provider.dto.ProviderModelResponse;
import com.nexoia.provider.model.ProviderConfiguration;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.repository.ProviderConfigurationRepository;
import com.nexoia.provider.service.OllamaProviderService;
import com.nexoia.provider.service.ProviderEndpointGuard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModelRequestExecutionPlannerTest {

    @Mock private ConversationRepository conversations;
    @Mock private ConversationMessageRepository messages;
    @Mock private ProviderConfigurationRepository providers;
    @Mock private ProviderEndpointGuard endpointGuard;
    @Mock private OllamaProviderService ollama;

    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();
    private ModelRequestExecutionPlanner planner;

    @BeforeEach
    void setUp() {
        planner = new ModelRequestExecutionPlanner(
                conversations, messages, providers, endpointGuard, ollama);
        when(conversations.findByIdAndUserIdAndArchivedFalse(conversationId, userId))
                .thenReturn(Optional.of(Conversation.builder()
                        .id(conversationId)
                        .userId(userId)
                        .title("Workspace chat")
                        .providerConfigurationId(providerId)
                        .selectedModel("qwen2.5vl:7b")
                        .workspaceId(UUID.randomUUID())
                        .build()));
        when(providers.findByIdAndUserId(providerId, userId)).thenReturn(Optional.of(
                ProviderConfiguration.builder()
                        .id(providerId)
                        .userId(userId)
                        .providerType(ProviderType.OLLAMA)
                        .endpoint("http://127.0.0.1:11434")
                        .displayName("Local")
                        .selectedModel("qwen3:8b")
                        .enabled(true)
                        .build()));
        when(ollama.models("http://127.0.0.1:11434")).thenReturn(List.of(
                new ProviderModelResponse("qwen2.5vl:7b", null, 5_969_245_856L, false, false),
                new ProviderModelResponse("qwen3:8b", null, 5_225_388_164L, true, true),
                new ProviderModelResponse("granite4.1:8b", null, 5_347_933_017L, true, false)));
    }

    @Test
    void promotesAWorkspaceWriteContinuationAndSelectsACompatibleExecutor() {
        when(messages.findContextHistory(eq(conversationId), any())).thenReturn(List.of(
                ConversationMessage.builder()
                        .role(ConversationRole.USER)
                        .content("Coloque o arquivo index.html na raiz do projeto")
                        .build(),
                ConversationMessage.builder()
                        .role(ConversationRole.ASSISTANT)
                        .content("Use este comando no terminal")
                        .build()));

        ModelRequestExecutionPlan execution = planner.plan(
                userId,
                conversationId,
                "eu quero que voce faca isso pra mim, pode criar",
                ConversationMode.CHAT,
                false);

        assertThat(execution.mode()).isEqualTo(ConversationMode.AGENT);
        assertThat(execution.model()).isEqualTo("qwen3:8b");
        assertThat(execution.effectiveObjective()).contains("index.html");
        assertThat(execution.automaticallyPromoted()).isTrue();
        assertThat(execution.executionModelChanged()).isTrue();
    }

    @Test
    void promotesProjectAnalysisAndPrefersAThinkingCapableExecutorWhenRequested() {
        when(messages.findContextHistory(eq(conversationId), any())).thenReturn(List.of());

        ModelRequestExecutionPlan execution = planner.plan(
                userId,
                conversationId,
                "analisa esse projeto pra mim",
                ConversationMode.CHAT,
                true);

        assertThat(execution.mode()).isEqualTo(ConversationMode.AGENT);
        assertThat(execution.model()).isEqualTo("qwen3:8b");
    }

    @Test
    void rejectsAgentWorkBeforeInferenceWhenTheProviderHasNoToolModel() {
        when(messages.findContextHistory(eq(conversationId), any())).thenReturn(List.of());
        when(ollama.models("http://127.0.0.1:11434")).thenReturn(List.of(
                new ProviderModelResponse("qwen2.5vl:7b", null, 5_969_245_856L, false, false)));

        assertThatThrownBy(() -> planner.plan(
                userId,
                conversationId,
                "analisa esse projeto pra mim",
                ConversationMode.CHAT,
                false))
                .isInstanceOf(AgentCapableModelUnavailableException.class);
    }
}
