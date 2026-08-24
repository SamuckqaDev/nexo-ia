package com.nexoia.provider.dto;

import com.nexoia.conversation.inference.model.AgentPlanStepStatus;

/** Provider-neutral, validated plan step emitted by the Agent plan tool. */
public record AgentPlanStepUpdate(
        String step,
        AgentPlanStepStatus status) {}
