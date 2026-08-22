package com.nexoia.conversation.inference.model;

/** Durable public lifecycle of the first governed agent runtime. */
public enum AgentState {
    PLANNING,
    RUNNING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED,
    BLOCKED
}
