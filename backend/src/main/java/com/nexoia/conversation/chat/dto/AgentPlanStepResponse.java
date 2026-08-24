package com.nexoia.conversation.chat.dto;

import com.nexoia.conversation.inference.model.AgentPlanStepStatus;

public record AgentPlanStepResponse(
        String step,
        AgentPlanStepStatus status) {}
