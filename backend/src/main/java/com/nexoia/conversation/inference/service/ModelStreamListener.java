package com.nexoia.conversation.inference.service;

import com.nexoia.conversation.inference.dto.event.CancelledEvent;
import com.nexoia.conversation.inference.dto.event.CompletedEvent;
import com.nexoia.conversation.inference.dto.event.StartedEvent;
import com.nexoia.conversation.inference.dto.event.StreamErrorEvent;
import com.nexoia.conversation.inference.dto.event.ThinkingEvent;
import com.nexoia.conversation.inference.dto.event.TokenEvent;
import com.nexoia.conversation.inference.dto.event.UsageEvent;

/**
 * Receives the ordered events of one model request.
 *
 * <p>The inference service owns the lifecycle and produces typed events; a transport such as SSE
 * only forwards them. Exactly one terminal event is delivered: completed, cancelled, or error.
 */
public interface ModelStreamListener {

    void onStarted(StartedEvent event);

    void onThinking(ThinkingEvent event);

    void onToken(TokenEvent event);

    void onUsage(UsageEvent event);

    void onCompleted(CompletedEvent event);

    void onCancelled(CancelledEvent event);

    void onError(StreamErrorEvent event);
}
