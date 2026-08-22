package com.nexoia.conversation.chat.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/** Determines whether a request is a normal answer or a governed read-only agent run. */
public enum ConversationMode {
    CHAT,
    AGENT;

    @JsonCreator
    public static ConversationMode from(String value) {
        if (value == null || value.isBlank()) {
            return CHAT;
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
