package com.nexoia.conversation.inference.dto;

import com.nexoia.conversation.chat.model.ConversationMode;

/** The authoritative request mode and provider model selected before reserving the stream. */
public record ModelRequestExecutionPlan(
        ConversationMode mode,
        String model,
        String effectiveObjective,
        boolean automaticallyPromoted,
        boolean executionModelChanged) {}
