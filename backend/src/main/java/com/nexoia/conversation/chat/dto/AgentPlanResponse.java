package com.nexoia.conversation.chat.dto;

import java.time.Instant;
import java.util.List;

public record AgentPlanResponse(
        int revision,
        String explanation,
        List<AgentPlanStepResponse> steps,
        Instant updatedAt) {}
