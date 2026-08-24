package com.nexoia.provider.dto;

import java.util.UUID;

/** Server-created scope for the request-local Agent plan tool; none of these ids reach the model. */
public record AgentPlanToolScope(
        UUID userId,
        UUID assistantMessageId,
        UUID correlationId) {}
