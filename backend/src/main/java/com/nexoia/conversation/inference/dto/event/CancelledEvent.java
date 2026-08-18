package com.nexoia.conversation.inference.dto.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Terminal event for a cancelled generation. The partial content is reported honestly and is stored
 * under the CANCELLED status, never promoted to a completed answer.
 */
public record CancelledEvent(UUID messageId, String content, Instant cancelledAt) {}
