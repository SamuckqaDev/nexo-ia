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
        KnowledgeWriteToolScope knowledgeWriteToolScope,
        WorkspaceToolScope workspaceToolScope,
        ToolExecutionObserver toolExecutionObserver,
        AgentPlanUpdateObserver agentPlanUpdateObserver) {

    public ChatCompletionCommand(
            ProviderType providerType,
            String endpoint,
            String model,
            List<ChatCompletionMessage> messages,
            boolean thinkingEnabled) {
        this(providerType, endpoint, model, messages, thinkingEnabled,
                ConversationMode.CHAT, null, null, null, null, null, null,
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
                knowledgeToolScope, null, null, null, null, null,
                toolExecutionObserver, AgentPlanUpdateObserver.NOOP);
    }

    /** Backward-compatible constructor for callers with no attached workspace scope. */
    public ChatCompletionCommand(
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
            KnowledgeWriteToolScope knowledgeWriteToolScope,
            ToolExecutionObserver toolExecutionObserver,
            AgentPlanUpdateObserver agentPlanUpdateObserver) {
        this(providerType, endpoint, model, messages, thinkingEnabled, mode,
                knowledgeToolScope, agentPlanToolScope, memoryToolScope, mcpToolScope,
                knowledgeWriteToolScope, null, toolExecutionObserver, agentPlanUpdateObserver);
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
                knowledgeToolScope, agentPlanToolScope, null, mcpToolScope, null, null,
                toolExecutionObserver, agentPlanUpdateObserver);
    }

    public ChatCompletionCommand withToolExecutionObserver(ToolExecutionObserver observer) {
        return new ChatCompletionCommand(
                providerType, endpoint, model, messages, thinkingEnabled,
                mode, knowledgeToolScope, agentPlanToolScope, memoryToolScope, mcpToolScope,
                knowledgeWriteToolScope, workspaceToolScope, observer, agentPlanUpdateObserver);
    }

    public ChatCompletionCommand withExecutionObservers(
            ToolExecutionObserver toolObserver,
            AgentPlanUpdateObserver planObserver) {
        return new ChatCompletionCommand(
                providerType, endpoint, model, messages, thinkingEnabled,
                mode, knowledgeToolScope, agentPlanToolScope, memoryToolScope, mcpToolScope,
                knowledgeWriteToolScope, workspaceToolScope, toolObserver, planObserver);
    }
}
