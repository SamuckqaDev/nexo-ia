package com.nexoia.conversation.inference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.nexoia.conversation.chat.model.ConversationMessage;
import com.nexoia.conversation.chat.model.ConversationRole;
import com.nexoia.conversation.chat.model.MessageStatus;
import com.nexoia.conversation.chat.repository.ConversationMessageRepository;
import com.nexoia.conversation.inference.config.ConversationContextProperties;
import com.nexoia.provider.dto.ChatCompletionMessage;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversationContextAssemblerTest {

    @Mock
    private ConversationMessageRepository messages;

    private final UUID conversationId = UUID.randomUUID();
    private final AtomicInteger sequence = new AtomicInteger();

    @Test
    void keepsTheWholeHistoryWhenItFitsTheBudget() {
        given(List.of(
                message(ConversationRole.USER, "first question"),
                message(ConversationRole.ASSISTANT, "first answer"),
                message(ConversationRole.USER, "second question")));

        List<ChatCompletionMessage> context = assembler(1000).assemble(conversationId);

        assertThat(context).extracting(ChatCompletionMessage::role)
                .containsExactly("user", "assistant", "user");
        assertThat(context.getLast().content()).isEqualTo("second question");
    }

    @Test
    void dropsTheOldestTurnsFirstWhenTheBudgetIsTight() {
        given(List.of(
                message(ConversationRole.USER, "a".repeat(400)),
                message(ConversationRole.ASSISTANT, "b".repeat(400)),
                message(ConversationRole.USER, "c".repeat(40))));

        // 200 tokens of budget at 4 characters per token fits the newest turn plus one more.
        List<ChatCompletionMessage> context = assembler(110).assemble(conversationId);

        assertThat(context).hasSize(2);
        assertThat(context.getFirst().content()).startsWith("b");
        assertThat(context.getLast().content()).startsWith("c");
    }

    @Test
    void alwaysSendsTheNewestMessageEvenWhenItAloneExceedsTheBudget() {
        given(List.of(
                message(ConversationRole.USER, "old"),
                message(ConversationRole.USER, "x".repeat(4000))));

        List<ChatCompletionMessage> context = assembler(10).assemble(conversationId);

        assertThat(context).hasSize(1);
        assertThat(context.getFirst().content()).hasSize(4000);
    }

    @Test
    void skipsTheEmptyPlaceholderOfAReservedAssistantMessage() {
        given(List.of(
                message(ConversationRole.USER, "question"),
                message(ConversationRole.ASSISTANT, "")));

        List<ChatCompletionMessage> context = assembler(1000).assemble(conversationId);

        assertThat(context).hasSize(1);
        assertThat(context.getFirst().content()).isEqualTo("question");
    }

    @Test
    void asksTheRepositoryOnlyForUsableHistory() {
        given(List.of(message(ConversationRole.USER, "question")));

        assembler(1000).assemble(conversationId);

        org.mockito.Mockito.verify(messages).findContextHistory(
                eq(conversationId),
                org.mockito.ArgumentMatchers.argThat(statuses ->
                        statuses.containsAll(List.of(MessageStatus.COMPLETED, MessageStatus.CANCELLED))
                                && !statuses.contains(MessageStatus.FAILED)));
    }

    private ConversationContextAssembler assembler(int tokenBudget) {
        return new ConversationContextAssembler(
                messages, new ConversationContextProperties(tokenBudget, 4));
    }

    private void given(List<ConversationMessage> history) {
        when(messages.findContextHistory(eq(conversationId), any())).thenReturn(history);
    }

    private ConversationMessage message(ConversationRole role, String content) {
        return ConversationMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .sequenceNumber(sequence.incrementAndGet())
                .role(role)
                .status(MessageStatus.COMPLETED)
                .content(content)
                .build();
    }
}
