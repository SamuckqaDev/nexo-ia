package com.nexoia.conversation.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.conversation.chat.dto.ConversationMessageResponse;
import com.nexoia.conversation.chat.dto.CreateConversationRequest;
import com.nexoia.conversation.chat.dto.RenameConversationRequest;
import com.nexoia.conversation.chat.dto.UpdateConversationModelRequest;
import com.nexoia.conversation.chat.exception.ConversationNotFoundException;
import com.nexoia.conversation.chat.model.Conversation;
import com.nexoia.conversation.chat.model.ConversationMessage;
import com.nexoia.conversation.chat.model.ConversationRole;
import com.nexoia.conversation.chat.model.MessageStatus;
import com.nexoia.conversation.chat.repository.ConversationMessageRepository;
import com.nexoia.conversation.chat.repository.ConversationRepository;
import com.nexoia.conversation.inference.config.ConversationContextProperties;
import com.nexoia.conversation.inference.repository.AgentPlanRepository;
import com.nexoia.conversation.inference.repository.ToolExecutionRepository;
import com.nexoia.provider.exception.ProviderConfigurationNotFoundException;
import com.nexoia.provider.repository.ProviderConfigurationRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversations;
    @Mock
    private ConversationMessageRepository messages;
    @Mock
    private ToolExecutionRepository toolExecutions;
    @Mock
    private AgentPlanRepository agentPlans;
    @Mock
    private ProviderConfigurationRepository providers;
    @Mock
    private AuditService audit;
    @Mock
    private ConversationKnowledgeService knowledge;
    private ConversationService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ConversationService(
                conversations, messages, toolExecutions, agentPlans, providers, audit,
                new ConversationContextProperties(8000, 4), knowledge);
    }

    @Test
    void createsAConversationOwnedByTheAuthenticatedUser() {
        when(conversations.save(any(Conversation.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.create(userId, new CreateConversationRequest("  Planning  "));

        assertThat(response.title()).isEqualTo("Planning");
        assertThat(response.selectedModel()).isNull();
    }

    @Test
    void appendsTheNextSequenceNumberAfterTheCurrentHighest() {
        when(conversations.findOwnedForUpdate(conversationId, userId))
                .thenReturn(Optional.of(conversation()));
        when(messages.findHighestSequenceNumber(conversationId)).thenReturn(7);
        when(messages.save(any(ConversationMessage.class))).thenAnswer(call -> call.getArgument(0));

        ConversationMessageResponse response =
                service.addUserMessage(userId, conversationId, "  hello  ");

        assertThat(response.role()).isEqualTo(ConversationRole.USER);
        assertThat(response.content()).isEqualTo("hello");
    }

    @Test
    void takesTheConversationWriteLockBeforeAppendingAMessage() {
        when(conversations.findOwnedForUpdate(conversationId, userId))
                .thenReturn(Optional.of(conversation()));
        when(messages.findHighestSequenceNumber(conversationId)).thenReturn(0);
        when(messages.save(any(ConversationMessage.class))).thenAnswer(call -> call.getArgument(0));

        service.addUserMessage(userId, conversationId, "first");

        verify(conversations).findOwnedForUpdate(conversationId, userId);
        verify(conversations, never()).findByIdAndUserIdAndArchivedFalse(conversationId, userId);
    }

    @Test
    void hidesAnotherUsersConversationFromMessageReads() {
        when(conversations.findByIdAndUserIdAndArchivedFalse(conversationId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.messages(userId, conversationId))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    void restoresPersistedActiveExecutionAndItsContextBudget() {
        UUID messageId = UUID.randomUUID();
        when(conversations.findByIdAndUserIdAndArchivedFalse(conversationId, userId))
                .thenReturn(Optional.of(conversation()));
        when(messages.findAllByConversationIdOrderBySequenceNumberAsc(conversationId))
                .thenReturn(List.of(ConversationMessage.builder()
                        .id(messageId)
                        .conversationId(conversationId)
                        .sequenceNumber(2)
                        .role(ConversationRole.ASSISTANT)
                        .status(MessageStatus.STREAMING)
                        .content("")
                        .model("qwen3:8b")
                        .build()));

        ConversationMessageResponse response = service.messages(userId, conversationId).getFirst();

        assertThat(response.id()).isEqualTo(messageId);
        assertThat(response.status()).isEqualTo(MessageStatus.STREAMING);
        assertThat(response.contextTokenBudget()).isEqualTo(8000);
        assertThat(response.totalTokens()).isNull();
        assertThat(response.completedAt()).isNull();
    }

    @Test
    void exposesPerMessageInputOutputTotalAndContextUsage() {
        when(conversations.findByIdAndUserIdAndArchivedFalse(conversationId, userId))
                .thenReturn(Optional.of(conversation()));
        when(messages.findAllByConversationIdOrderBySequenceNumberAsc(conversationId))
                .thenReturn(List.of(ConversationMessage.builder()
                        .id(UUID.randomUUID())
                        .conversationId(conversationId)
                        .sequenceNumber(2)
                        .role(ConversationRole.ASSISTANT)
                        .status(MessageStatus.COMPLETED)
                        .content("answer")
                        .model("qwen3:8b")
                        .inputTokens(20)
                        .outputTokens(3)
                        .build()));

        ConversationMessageResponse response = service.messages(userId, conversationId).getFirst();

        assertThat(response.inputTokens()).isEqualTo(20);
        assertThat(response.outputTokens()).isEqualTo(3);
        assertThat(response.totalTokens()).isEqualTo(23L);
        assertThat(response.contextTokensUsed()).isEqualTo(20);
        assertThat(response.contextTokenBudget()).isEqualTo(8000);
    }

    @Test
    void hidesAnotherUsersConversationFromWrites() {
        when(conversations.findOwnedForUpdate(conversationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addUserMessage(userId, conversationId, "hello"))
                .isInstanceOf(ConversationNotFoundException.class);
        verify(messages, never()).save(any(ConversationMessage.class));
    }

    @Test
    void rejectsAProviderOwnedByAnotherUser() {
        UUID providerId = UUID.randomUUID();
        when(conversations.findOwnedForUpdate(conversationId, userId))
                .thenReturn(Optional.of(conversation()));
        when(providers.findByIdAndUserId(providerId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.selectModel(userId, conversationId,
                new UpdateConversationModelRequest(providerId, "llama3.2")))
                .isInstanceOf(ProviderConfigurationNotFoundException.class);
    }

    @Test
    void renamesAndArchivesOnlyOwnedConversations() {
        Conversation conversation = conversation();
        when(conversations.findOwnedForUpdate(conversationId, userId))
                .thenReturn(Optional.of(conversation));

        service.rename(userId, conversationId, new RenameConversationRequest("  Renamed  "));
        assertThat(conversation.getTitle()).isEqualTo("Renamed");

        service.archive(userId, conversationId);
        assertThat(conversation.isArchived()).isTrue();
    }

    private Conversation conversation() {
        return Conversation.builder()
                .id(conversationId)
                .userId(userId)
                .title("Planning")
                .archived(false)
                .build();
    }
}
