package com.nexoia.conversation.chat.exception;

import com.nexoia.shared.exception.ConflictApplicationException;

public class ConversationBusyException extends ConflictApplicationException {

    public ConversationBusyException() {
        super("This conversation already has a model request in progress");
    }
}
