package com.nexoia.provider.dto;

import java.time.Instant;
import java.util.List;

/** One complete replacement revision of an Agent implementation plan. */
public record AgentPlanUpdate(
        int revision,
        String explanation,
        List<AgentPlanStepUpdate> steps,
        Instant updatedAt) {

    public AgentPlanUpdate {
        steps = List.copyOf(steps);
    }
}
