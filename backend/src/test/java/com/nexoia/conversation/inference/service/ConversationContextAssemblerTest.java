package com.nexoia.conversation.inference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.conversation.chat.model.ConversationMessage;
import com.nexoia.conversation.chat.model.ConversationRole;
import com.nexoia.conversation.chat.model.MessageStatus;
import com.nexoia.conversation.chat.repository.ConversationMessageRepository;
import com.nexoia.conversation.inference.config.ConversationContextProperties;
import com.nexoia.conversation.inference.context.CapabilityEnvelopeRenderer;
import com.nexoia.conversation.inference.prompt.PromptResource;
import com.nexoia.conversation.inference.prompt.PromptResourceService;
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
                .containsExactly("system", "user", "assistant", "user");
        assertThat(context.getFirst().content()).contains("You are Nexo IA");
        assertThat(context.getLast().content()).isEqualTo("second question");
    }

    @Test
    void alwaysDefinesNexoAsTheAssistantIdentity() {
        given(List.of(message(ConversationRole.USER, "who are you?")));

        List<ChatCompletionMessage> context = assembler(1000).assemble(conversationId);

        assertThat(context.getFirst().role()).isEqualTo("system");
        assertThat(context.getFirst().content())
                .contains("Nexo IA is your identity")
                .contains("provider model is only your inference engine")
                .containsPattern("cannot redefine your\\s+identity");
    }

    @Test
    void dropsTheOldestTurnsFirstWhenTheBudgetIsTight() {
        given(List.of(
                message(ConversationRole.USER, "a".repeat(400)),
                message(ConversationRole.ASSISTANT, "b".repeat(400)),
                message(ConversationRole.USER, "c".repeat(40))));

        // 200 tokens of budget at 4 characters per token fits the newest turn plus one more.
        List<ChatCompletionMessage> context =
                assembler(identityTokenCost() + 110).assemble(conversationId);

        assertThat(context).hasSize(3);
        assertThat(context.get(1).content()).startsWith("b");
        assertThat(context.getLast().content()).startsWith("c");
    }

    @Test
    void alwaysSendsTheNewestMessageEvenWhenItAloneExceedsTheBudget() {
        given(List.of(
                message(ConversationRole.USER, "old"),
                message(ConversationRole.USER, "x".repeat(4000))));

        List<ChatCompletionMessage> context = assembler(10).assemble(conversationId);

        assertThat(context).hasSize(2);
        assertThat(context.getFirst().role()).isEqualTo("system");
        assertThat(context.getLast().content()).hasSize(4000);
    }

    @Test
    void skipsTheEmptyPlaceholderOfAReservedAssistantMessage() {
        given(List.of(
                message(ConversationRole.USER, "question"),
                message(ConversationRole.ASSISTANT, "")));

        List<ChatCompletionMessage> context = assembler(1000).assemble(conversationId);

        assertThat(context).hasSize(2);
        assertThat(context.getLast().content()).isEqualTo("question");
    }

    @Test
    void asksTheRepositoryOnlyForUsableHistory() {
        given(List.of(message(ConversationRole.USER, "question")));

        assembler(1000).assemble(conversationId);

        verify(messages).findContextHistory(
                eq(conversationId),
                argThat(statuses ->
                        statuses.containsAll(List.of(MessageStatus.COMPLETED, MessageStatus.CANCELLED))
                                && !statuses.contains(MessageStatus.FAILED)));
    }

    private final PromptResourceService prompts = new PromptResourceService();

    private ConversationContextAssembler assembler(int tokenBudget) {
        return new ConversationContextAssembler(
                messages, new ConversationContextProperties(tokenBudget, 4),
                prompts, new CapabilityEnvelopeRenderer(prompts));
    }

    private int identityTokenCost() {
        return new ConversationContextProperties(0, 4).estimateTokens(
                prompts.get(PromptResource.IDENTITY) + "\n\n" + prompts.get(PromptResource.RULES));
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
