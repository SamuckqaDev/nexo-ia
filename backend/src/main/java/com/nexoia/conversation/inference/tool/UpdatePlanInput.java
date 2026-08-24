package com.nexoia.conversation.inference.tool;

import java.util.List;
import org.springframework.ai.tool.annotation.ToolParam;

/** Model-facing input for replacing the visible plan; server and user identifiers are absent. */
public record UpdatePlanInput(
        @ToolParam(description = "Optional concise reason for this plan revision", required = false)
        String explanation,
        @ToolParam(description = "Complete ordered plan, with no more than twelve steps")
        List<UpdatePlanStepInput> plan) {}
