package com.nexoia.conversation.inference.dto.event;

import java.time.Instant;
import java.util.UUID;

public record CompletedEvent(UUID messageId, String content, Instant completedAt) {}
