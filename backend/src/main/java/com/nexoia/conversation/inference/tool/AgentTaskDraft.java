package com.nexoia.conversation.inference.tool;

/** One deterministic, user-visible task prepared before model execution starts. */
public record AgentTaskDraft(
        String title,
        String description,
        String requiredToolPrefix) {}
