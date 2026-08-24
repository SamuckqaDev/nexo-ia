package com.nexoia.conversation.inference.dto.event;

import java.time.Instant;
import java.util.List;

public record PlanUpdatedEvent(
        int revision,
        String explanation,
        List<AgentPlanStepEvent> steps,
        Instant updatedAt) {}
