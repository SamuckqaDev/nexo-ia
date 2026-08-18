package com.nexoia.conversation.chat.model;

import java.util.Set;

/**
 * Lifecycle of a message. A user message is stored terminal; an assistant message walks the model
 * request states defined by the release 0.1 contract.
 */
public enum MessageStatus {
    QUEUED,
    STREAMING,
    CANCELLING,
    COMPLETED,
    CANCELLED,
    FAILED;

    private static final Set<MessageStatus> ACTIVE = Set.of(QUEUED, STREAMING, CANCELLING);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }

    public boolean isTerminal() {
        return !isActive();
    }
}
