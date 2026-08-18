package com.nexoia.conversation.inference.dto.event;

import java.util.UUID;

/**
 * Terminal event for a failed generation. Carries a safe code and message only; provider hosts,
 * stack traces, and third-party error text never reach the client.
 */
public record StreamErrorEvent(UUID messageId, String code, String message) {}
