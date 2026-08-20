package com.nexoia.conversation.inference.controller;

import com.nexoia.conversation.inference.dto.event.CancelledEvent;
import com.nexoia.conversation.inference.dto.event.CompletedEvent;
import com.nexoia.conversation.inference.dto.event.StartedEvent;
import com.nexoia.conversation.inference.dto.event.StreamErrorEvent;
import com.nexoia.conversation.inference.dto.event.ThinkingEvent;
import com.nexoia.conversation.inference.dto.event.TokenEvent;
import com.nexoia.conversation.inference.dto.event.UsageEvent;
import com.nexoia.conversation.inference.service.ModelStreamListener;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Forwards model request events to an SSE response.
 *
 * <p>Once the response has been committed the HTTP status can no longer change, so a failure is
 * reported as a typed error event and the stream is closed cleanly. The response connection is not
 * the execution owner: navigating away may close SSE, but the model request keeps running and its
 * authoritative state remains available through the conversation messages endpoint.
 */
@Slf4j
@RequiredArgsConstructor
public class SseModelStreamListener implements ModelStreamListener {

    private final SseEmitter emitter;
    private final UUID assistantMessageId;
    private boolean connected = true;

    @Override
    public void onStarted(StartedEvent event) {
        send("started", event);
    }

    @Override
    public void onThinking(ThinkingEvent event) {
        send("thinking", event);
    }

    @Override
    public void onToken(TokenEvent event) {
        send("token", event);
    }

    @Override
    public void onUsage(UsageEvent event) {
        send("usage", event);
    }

    @Override
    public void onCompleted(CompletedEvent event) {
        if (send("completed", event)) {
            emitter.complete();
        }
    }

    @Override
    public void onCancelled(CancelledEvent event) {
        if (send("cancelled", event)) {
            emitter.complete();
        }
    }

    @Override
    public void onError(StreamErrorEvent event) {
        if (send("error", event)) {
            emitter.complete();
        }
    }

    private boolean send(String name, Object payload) {
        if (!connected) {
            return false;
        }

        try {
            emitter.send(SseEmitter.event().name(name).data(payload));
            return true;
        } catch (IOException | IllegalStateException exception) {
            connected = false;
            log.debug("[NEXO-BACK][INFERENCE] SSE client disconnected, execution continues messageId={}",
                    assistantMessageId);
            return false;
        }
    }
}
