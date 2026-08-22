package com.nexoia.conversation.inference.dto.event;

import java.time.Instant;
import java.util.UUID;

public record ToolStartedEvent(
        UUID executionId,
        String toolName,
        Instant startedAt) {}
