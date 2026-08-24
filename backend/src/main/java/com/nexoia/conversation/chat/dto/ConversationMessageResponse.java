package com.nexoia.conversation.chat.dto;

import com.nexoia.conversation.chat.model.ConversationRole;
import com.nexoia.conversation.chat.model.ConversationMode;
import com.nexoia.conversation.chat.model.MessageStatus;
import com.nexoia.conversation.inference.model.AgentState;
import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.provider.model.ProcessingLocation;
import com.nexoia.provider.model.TokenSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationMessageResponse(
        UUID id,
        ConversationRole role,
        MessageStatus status,
        String content,
        String model,
        Integer inputTokens,
        Integer outputTokens,
        Long totalTokens,
        Integer contextTokensUsed,
        Integer contextTokenBudget,
        TokenSource tokenSource,
        Long latencyMs,
        ProcessingLocation processingLocation,
        String failureCode,
        ConversationMode mode,
        AgentState agentState,
        Instant createdAt,
        Instant completedAt,
        List<CitationResponse> citations,
        List<ToolExecutionResponse> toolExecutions,
        AgentPlanResponse agentPlan) {}
