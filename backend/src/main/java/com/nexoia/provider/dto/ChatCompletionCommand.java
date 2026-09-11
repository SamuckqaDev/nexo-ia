package com.nexoia.provider.dto;

import com.nexoia.conversation.chat.model.ConversationMode;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.secret.dto.ProviderAuthentication;
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
        AgentPlanUpdateObserver agentPlanUpdateObserver,
        ProviderAuthentication authentication,
        String fallbackModel,
        AgentExecutionDirective agentExecution) {

    public ChatCompletionCommand {
        authentication = authentication == null ? ProviderAuthentication.none() : authentication;
    }

    /** Backward-compatible constructor for callers that do not supply provider credentials. */
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
            WorkspaceToolScope workspaceToolScope,
            ToolExecutionObserver toolExecutionObserver,
            AgentPlanUpdateObserver agentPlanUpdateObserver) {
        this(providerType, endpoint, model, messages, thinkingEnabled, mode,
                knowledgeToolScope, agentPlanToolScope, memoryToolScope, mcpToolScope,
                knowledgeWriteToolScope, workspaceToolScope, toolExecutionObserver,
                agentPlanUpdateObserver, ProviderAuthentication.none(), null, null);
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
            MemoryToolScope memoryToolScope,
            McpToolScope mcpToolScope,
            KnowledgeWriteToolScope knowledgeWriteToolScope,
            WorkspaceToolScope workspaceToolScope,
            ToolExecutionObserver toolExecutionObserver,
            AgentPlanUpdateObserver agentPlanUpdateObserver,
            ProviderAuthentication authentication) {
        this(providerType, endpoint, model, messages, thinkingEnabled, mode,
                knowledgeToolScope, agentPlanToolScope, memoryToolScope, mcpToolScope,
                knowledgeWriteToolScope, workspaceToolScope, toolExecutionObserver,
                agentPlanUpdateObserver, authentication, null, null);
    }

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
                knowledgeWriteToolScope, workspaceToolScope, observer, agentPlanUpdateObserver,
                authentication, fallbackModel, agentExecution);
    }

    public ChatCompletionCommand withExecutionObservers(
            ToolExecutionObserver toolObserver,
            AgentPlanUpdateObserver planObserver) {
        return new ChatCompletionCommand(
                providerType, endpoint, model, messages, thinkingEnabled,
                mode, knowledgeToolScope, agentPlanToolScope, memoryToolScope, mcpToolScope,
                knowledgeWriteToolScope, workspaceToolScope, toolObserver, planObserver,
                authentication, fallbackModel, agentExecution);
    }

    public ChatCompletionCommand withModel(String executionModel) {
        return new ChatCompletionCommand(
                providerType, endpoint, executionModel, messages, thinkingEnabled,
                mode, knowledgeToolScope, agentPlanToolScope, memoryToolScope, mcpToolScope,
                knowledgeWriteToolScope, workspaceToolScope, toolExecutionObserver,
                agentPlanUpdateObserver, authentication, null, agentExecution);
    }

    public ChatCompletionCommand forPlanning(AgentPlanUpdateObserver planObserver) {
        return new ChatCompletionCommand(
                providerType, endpoint, model, messages, thinkingEnabled,
                ConversationMode.AGENT, null, agentPlanToolScope, null, null, null, null,
                toolExecutionObserver, planObserver, authentication, fallbackModel,
                AgentExecutionDirective.planning());
    }

    public ChatCompletionCommand forTask(
            AgentExecutionDirective directive,
            List<ChatCompletionMessage> taskMessages) {
        boolean toolRequired = directive.requiredToolPrefix() != null
                && !directive.requiredToolPrefix().isBlank();
        return new ChatCompletionCommand(
                providerType, endpoint, model, taskMessages, thinkingEnabled,
                ConversationMode.AGENT,
                toolRequired ? knowledgeToolScope : null,
                null,
                toolRequired ? memoryToolScope : null,
                toolRequired ? mcpToolScope : null,
                toolRequired ? knowledgeWriteToolScope : null,
                toolRequired ? workspaceToolScope : null,
                toolExecutionObserver,
                AgentPlanUpdateObserver.NOOP, authentication, fallbackModel, directive);
    }

    public ChatCompletionCommand forSynthesis(List<ChatCompletionMessage> synthesisMessages) {
        return new ChatCompletionCommand(
                providerType, endpoint, model, synthesisMessages, thinkingEnabled,
                ConversationMode.CHAT, null, null, null, null, null, null,
                ToolExecutionObserver.NOOP, AgentPlanUpdateObserver.NOOP,
                authentication, null, null);
    }
}
