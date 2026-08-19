package com.nexoia.conversation.inference.dto;

import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.model.ProcessingLocation;
import java.util.UUID;

/**
 * Everything the streaming stage needs after the short reservation transaction has committed.
 *
 * <p>It carries values rather than entities on purpose: the streaming stage runs outside a
 * transaction and must never touch a detached persistence context.
 */
public record ModelRequestReservation(
        UUID userId,
        UUID userMessageId,
        UUID assistantMessageId,
        UUID correlationId,
        ChatCompletionCommand command,
        ProcessingLocation processingLocation) {}
