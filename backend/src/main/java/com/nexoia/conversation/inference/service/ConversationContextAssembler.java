package com.nexoia.conversation.inference.service;

import com.nexoia.conversation.chat.model.ConversationMessage;
import com.nexoia.conversation.chat.model.ConversationRole;
import com.nexoia.conversation.chat.model.MessageStatus;
import com.nexoia.conversation.chat.repository.ConversationMessageRepository;
import com.nexoia.conversation.inference.config.ConversationContextProperties;
import com.nexoia.provider.dto.ChatCompletionMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Builds the message history sent to a model, bounded by an explicit token budget.
 *
 * <p>Only the requested conversation is read, and only messages that actually became part of it:
 * a failed generation is excluded, while a cancelled one keeps the partial text the user saw.
 */
@Service
@RequiredArgsConstructor
public class ConversationContextAssembler {

    private static final Set<MessageStatus> USABLE =
            Set.of(MessageStatus.COMPLETED, MessageStatus.CANCELLED);

    private final ConversationMessageRepository messages;
    private final ConversationContextProperties properties;

    public List<ChatCompletionMessage> assemble(UUID conversationId) {
        List<ConversationMessage> history = messages.findContextHistory(conversationId, USABLE);
        List<ChatCompletionMessage> selected = new ArrayList<>();
        int budget = properties.tokenBudget();
        int used = 0;

        // Walk backwards so the most recent turns survive a tight budget.
        for (int index = history.size() - 1; index >= 0; index--) {
            ConversationMessage message = history.get(index);
            if (message.getContent() == null || message.getContent().isBlank()) {
                continue;
            }

            int cost = properties.estimateTokens(message.getContent());
            boolean isNewestMessage = selected.isEmpty();

            // The newest message is the request itself: dropping it would send an empty prompt.
            if (!isNewestMessage && used + cost > budget) {
                break;
            }

            selected.add(new ChatCompletionMessage(role(message), message.getContent()));
            used += cost;
        }

        return selected.reversed();
    }

    private String role(ConversationMessage message) {
        return message.getRole() == ConversationRole.USER ? "user" : "assistant";
    }
}
