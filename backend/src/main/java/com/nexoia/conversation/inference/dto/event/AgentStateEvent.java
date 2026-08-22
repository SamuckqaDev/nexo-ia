package com.nexoia.conversation.inference.dto.event;

import com.nexoia.conversation.inference.model.AgentState;
import java.time.Instant;

public record AgentStateEvent(AgentState state, Instant changedAt) {}
