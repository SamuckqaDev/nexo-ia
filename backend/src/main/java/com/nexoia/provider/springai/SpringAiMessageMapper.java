package com.nexoia.provider.springai;

import com.nexoia.provider.dto.ChatCompletionMessage;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

/**
 * Maps Nexo's provider-neutral messages to Spring AI messages, preserving role and order. Nexo owns
 * how the context is assembled and bounded; this only translates the already-built list so Spring AI
 * can prompt the model. An unknown role is a programming error, not user input, so it fails loudly.
 */
@Component
public class SpringAiMessageMapper {

    public List<Message> toSpringAi(List<ChatCompletionMessage> messages) {
        return messages.stream().map(this::toSpringAi).toList();
    }

    private Message toSpringAi(ChatCompletionMessage message) {
        return switch (message.role()) {
            case "system" -> new SystemMessage(message.content());
            case "user" -> new UserMessage(message.content());
            case "assistant" -> new AssistantMessage(message.content());
            default -> throw new IllegalArgumentException("Unsupported message role: " + message.role());
        };
    }
}
