package com.nexoia.conversation.inference.tool;

import com.nexoia.conversation.inference.model.AgentPlanStepStatus;
import org.springframework.ai.tool.annotation.ToolParam;

/** One model-proposed, user-visible step. No hidden reasoning belongs in this contract. */
public record UpdatePlanStepInput(
        @ToolParam(description = "Concise action title") String step,
        @ToolParam(
                description = "One sentence explaining what will be done and what observable result is expected",
                required = false)
        String description,
        @ToolParam(description = "PENDING, IN_PROGRESS, or COMPLETED") AgentPlanStepStatus status) {}
