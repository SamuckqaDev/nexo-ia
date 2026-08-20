package com.nexoia.conversation.inference.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nexoia.conversation.inference.dto.event.TokenEvent;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseModelStreamListenerTest {

    @Test
    void stopsWritingButDoesNotOwnOrCancelExecutionAfterClientDisconnects() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("reader gone"))
                .when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        SseModelStreamListener listener =
                new SseModelStreamListener(emitter, UUID.randomUUID());

        listener.onToken(new TokenEvent("first", 0));
        listener.onToken(new TokenEvent("second", 1));

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }
}
