package com.nexoia.conversation.inference.controller;

import com.nexoia.conversation.inference.dto.event.CancelledEvent;
import com.nexoia.conversation.inference.dto.event.CompletedEvent;
import com.nexoia.conversation.inference.dto.event.StartedEvent;
import com.nexoia.conversation.inference.dto.event.StreamErrorEvent;
import com.nexoia.conversation.inference.dto.event.ThinkingEvent;
import com.nexoia.conversation.inference.dto.event.TokenEvent;
import com.nexoia.conversation.inference.dto.event.UsageEvent;
import com.nexoia.conversation.inference.service.ModelRequestRegistry;
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
 * reported as a typed error event and the stream is closed cleanly. If the client has gone away,
 * writing fails; that is treated as a cancellation so the generation stops instead of running on
 * without a reader.
 */
@Slf4j
@RequiredArgsConstructor
public class SseModelStreamListener implements ModelStreamListener {

    private final SseEmitter emitter;
    private final ModelRequestRegistry registry;
    private final UUID assistantMessageId;

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
        send("completed", event);
        emitter.complete();
    }

    @Override
    public void onCancelled(CancelledEvent event) {
        send("cancelled", event);
        emitter.complete();
    }

    @Override
    public void onError(StreamErrorEvent event) {
        send("error", event);
        emitter.complete();
    }

    private void send(String name, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(name).data(payload));
        } catch (IOException | IllegalStateException exception) {
            // The reader is gone. Stop the generation rather than producing tokens nobody receives.
            log.debug("[NEXO-BACK][INFERENCE] SSE client disconnected, cancelling messageId={}",
                    assistantMessageId);
            registry.requestCancellation(assistantMessageId);
        }
    }
}
