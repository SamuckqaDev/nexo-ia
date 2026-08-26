package com.nexoia.conversation.inference.model;

/** Persistable, user-visible Agent plan step. */
public record AgentPlanStep(
        String step,
        String description,
        AgentPlanStepStatus status) {}
