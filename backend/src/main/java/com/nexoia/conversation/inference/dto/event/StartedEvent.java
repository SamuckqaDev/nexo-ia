package com.nexoia.conversation.inference.dto.event;

import com.nexoia.provider.model.ProcessingLocation;
import java.util.UUID;

/**
 * First event of a model request. It gives the client the identifiers it needs to cancel the run and
 * to reconcile its optimistic state with persisted messages.
 */
public record StartedEvent(
        UUID userMessageId,
        UUID assistantMessageId,
        UUID correlationId,
        String model,
        ProcessingLocation processingLocation) {}
