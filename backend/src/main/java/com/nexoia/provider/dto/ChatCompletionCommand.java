package com.nexoia.provider.dto;

import com.nexoia.conversation.chat.model.ConversationMode;
import com.nexoia.provider.model.ProviderType;
import java.util.List;

/**
 * A provider-neutral inference request. The endpoint is the user's own registered provider, so it
 * must already have passed the endpoint guard before reaching a client.
 */
public record ChatCompletionCommand(
        ProviderType providerType,
        String endpoint,
        String model,
        List<ChatCompletionMessage> messages,
        boolean thinkingEnabled,
        ConversationMode mode,
        KnowledgeToolScope knowledgeToolScope,
        ToolExecutionObserver toolExecutionObserver) {

    public ChatCompletionCommand(
            ProviderType providerType,
            String endpoint,
            String model,
            List<ChatCompletionMessage> messages,
            boolean thinkingEnabled) {
        this(providerType, endpoint, model, messages, thinkingEnabled,
                ConversationMode.CHAT, null, ToolExecutionObserver.NOOP);
    }

    public ChatCompletionCommand withToolExecutionObserver(ToolExecutionObserver observer) {
        return new ChatCompletionCommand(
                providerType, endpoint, model, messages, thinkingEnabled,
                mode, knowledgeToolScope, observer);
    }
}
