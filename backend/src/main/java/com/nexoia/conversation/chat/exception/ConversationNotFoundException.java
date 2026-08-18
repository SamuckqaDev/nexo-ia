package com.nexoia.conversation.chat.exception;

import com.nexoia.shared.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class ConversationNotFoundException extends ApplicationException {

    public ConversationNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Conversation not found");
    }
}
