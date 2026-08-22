package com.nexoia.provider.dto;

/** Safe terminal state exposed for a governed tool execution. */
public enum ToolExecutionStatus {
    RUNNING,
    FOUND,
    NO_RESULTS,
    UNAVAILABLE,
    DENIED
}
