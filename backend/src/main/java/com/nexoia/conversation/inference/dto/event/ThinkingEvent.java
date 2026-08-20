package com.nexoia.conversation.inference.dto.event;

/** A temporary provider reasoning delta that is streamed but never persisted as conversation data. */
public record ThinkingEvent(String content, int index) {}
