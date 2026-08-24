package com.nexoia.conversation.inference.tool;

import com.nexoia.provider.dto.ToolExecutionStatus;

/** Small tool result returned to the model after a visible plan revision. */
public record UpdatePlanResult(
        ToolExecutionStatus status,
        Integer revision,
        String message) {}
