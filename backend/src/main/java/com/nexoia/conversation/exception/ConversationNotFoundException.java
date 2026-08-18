package com.nexoia.conversation.exception;
import com.nexoia.shared.exception.UnauthorizedApplicationException;
public class ConversationNotFoundException extends UnauthorizedApplicationException { public ConversationNotFoundException() { super("Conversation is unavailable"); } }
