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
        AgentPlanToolScope agentPlanToolScope,
        MemoryToolScope memoryToolScope,
        McpToolScope mcpToolScope,
        ToolExecutionObserver toolExecutionObserver,
        AgentPlanUpdateObserver agentPlanUpdateObserver) {

    public ChatCompletionCommand(
            ProviderType providerType,
            String endpoint,
            String model,
            List<ChatCompletionMessage> messages,
            boolean thinkingEnabled) {
        this(providerType, endpoint, model, messages, thinkingEnabled,
                ConversationMode.CHAT, null, null, null, null,
                ToolExecutionObserver.NOOP, AgentPlanUpdateObserver.NOOP);
    }

    public ChatCompletionCommand(
            ProviderType providerType,
            String endpoint,
            String model,
            List<ChatCompletionMessage> messages,
            boolean thinkingEnabled,
            ConversationMode mode,
            KnowledgeToolScope knowledgeToolScope,
            ToolExecutionObserver toolExecutionObserver) {
        this(providerType, endpoint, model, messages, thinkingEnabled, mode,
                knowledgeToolScope, null, null, null,
                toolExecutionObserver, AgentPlanUpdateObserver.NOOP);
    }

    public ChatCompletionCommand(
            ProviderType providerType,
            String endpoint,
            String model,
            List<ChatCompletionMessage> messages,
            boolean thinkingEnabled,
            ConversationMode mode,
            KnowledgeToolScope knowledgeToolScope,
            AgentPlanToolScope agentPlanToolScope,
            McpToolScope mcpToolScope,
            ToolExecutionObserver toolExecutionObserver,
            AgentPlanUpdateObserver agentPlanUpdateObserver) {
        this(providerType, endpoint, model, messages, thinkingEnabled, mode,
                knowledgeToolScope, agentPlanToolScope, null, mcpToolScope,
                toolExecutionObserver, agentPlanUpdateObserver);
    }

    public ChatCompletionCommand withToolExecutionObserver(ToolExecutionObserver observer) {
        return new ChatCompletionCommand(
                providerType, endpoint, model, messages, thinkingEnabled,
                mode, knowledgeToolScope, agentPlanToolScope, memoryToolScope, mcpToolScope,
                observer, agentPlanUpdateObserver);
    }

    public ChatCompletionCommand withExecutionObservers(
            ToolExecutionObserver toolObserver,
            AgentPlanUpdateObserver planObserver) {
        return new ChatCompletionCommand(
                providerType, endpoint, model, messages, thinkingEnabled,
                mode, knowledgeToolScope, agentPlanToolScope, memoryToolScope, mcpToolScope,
                toolObserver, planObserver);
    }
}
