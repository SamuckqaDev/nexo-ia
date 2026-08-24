package com.nexoia.conversation.inference.dto.event;

import com.nexoia.conversation.inference.model.AgentPlanStepStatus;

public record AgentPlanStepEvent(
        String step,
        AgentPlanStepStatus status) {}
