package com.nexoia.conversation.inference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.conversation.inference.dto.ModelRequestReservation;
import com.nexoia.conversation.inference.dto.event.CancelledEvent;
import com.nexoia.conversation.inference.dto.event.CompletedEvent;
import com.nexoia.conversation.inference.dto.event.StartedEvent;
import com.nexoia.conversation.inference.dto.event.StreamErrorEvent;
import com.nexoia.conversation.inference.dto.event.TokenEvent;
import com.nexoia.conversation.inference.dto.event.UsageEvent;
import com.nexoia.conversation.inference.exception.UnsupportedProviderException;
import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionMessage;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.exception.ProviderStreamException;
import com.nexoia.provider.model.ProcessingLocation;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.model.TokenSource;
import com.nexoia.provider.service.ChatCompletionClient;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModelRequestServiceTest {

    @Mock
    private ModelRequestStore store;
    @Mock
    private ChatCompletionClient client;
    @Mock
    private com.nexoia.audit.service.AuditService audit;

    private final ModelRequestRegistry registry = new ModelRequestRegistry();
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-18T12:00:00Z"), ZoneOffset.UTC);
    private final RecordingListener listener = new RecordingListener();

    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID userMessageId = UUID.randomUUID();
    private final UUID assistantMessageId = UUID.randomUUID();

    private ModelRequestService service;

    @BeforeEach
    void setUp() {
        service = new ModelRequestService(store, registry, audit, List.of(client), clock);
    }

    @Test
    void emitsStartedTokensUsageAndCompletedForASuccessfulRequest() {
        reservationIsAvailable();
        when(client.supports(ProviderType.OLLAMA)).thenReturn(true);
        when(client.stream(any(), any(), any())).thenAnswer(call -> {
            Consumer<String> onToken = call.getArgument(1);
            onToken.accept("Hel");
            onToken.accept("lo");
            return new ChatCompletionOutcome("Hello", 20, 3, TokenSource.PROVIDER, false, "stop");
        });
        when(store.recordCompletion(eq(assistantMessageId), any(), anyLong()))
                .thenReturn(Instant.parse("2026-08-18T12:00:01Z"));

        service.stream(userId, conversationId, "hi", listener);

        assertThat(listener.started).isNotNull();
        assertThat(listener.started.assistantMessageId()).isEqualTo(assistantMessageId);
        assertThat(listener.tokens).extracting(TokenEvent::content).containsExactly("Hel", "lo");
        assertThat(listener.tokens).extracting(TokenEvent::index).containsExactly(0, 1);
        assertThat(listener.usage.inputTokens()).isEqualTo(20);
        assertThat(listener.usage.tokenSource()).isEqualTo(TokenSource.PROVIDER);
        assertThat(listener.completed.content()).isEqualTo("Hello");
        assertThat(listener.cancelled).isNull();
        assertThat(listener.error).isNull();
    }

    @Test
    void persistsThePartialAnswerAndEmitsCancelledWithoutCompleting() {
        reservationIsAvailable();
        when(client.supports(ProviderType.OLLAMA)).thenReturn(true);
        when(client.stream(any(), any(), any())).thenAnswer(call -> {
            Consumer<String> onToken = call.getArgument(1);
            onToken.accept("Hel");
            return new ChatCompletionOutcome("Hel", null, null, null, true, "cancelled");
        });
        when(store.recordCancellation(eq(assistantMessageId), eq("Hel"), anyLong()))
                .thenReturn(Instant.parse("2026-08-18T12:00:02Z"));

        service.stream(userId, conversationId, "hi", listener);

        assertThat(listener.cancelled.content()).isEqualTo("Hel");
        assertThat(listener.completed).isNull();
        assertThat(listener.usage).isNull();
        verify(store, never()).recordCompletion(any(), any(), anyLong());
    }

    @Test
    void recordsAFailureAndEmitsASafeErrorWhenTheProviderBreaks() {
        reservationIsAvailable();
        when(client.supports(ProviderType.OLLAMA)).thenReturn(true);
        when(client.stream(any(), any(), any()))
                .thenThrow(new ProviderStreamException(new IOException("connection refused to 10.0.0.9")));

        service.stream(userId, conversationId, "hi", listener);

        assertThat(listener.error.code()).isEqualTo("PROVIDER_STREAM_FAILED");
        assertThat(listener.error.message()).doesNotContain("10.0.0.9");
        assertThat(listener.completed).isNull();
        verify(store).recordFailure(eq(assistantMessageId), eq("PROVIDER_STREAM_FAILED"), anyLong());
    }

    @Test
    void releasesTheCancellationSignalAfterEveryOutcome() {
        reservationIsAvailable();
        when(client.supports(ProviderType.OLLAMA)).thenReturn(true);
        when(client.stream(any(), any(), any()))
                .thenReturn(new ChatCompletionOutcome("ok", 1, 1, TokenSource.PROVIDER, false, "stop"));

        service.stream(userId, conversationId, "hi", listener);

        assertThat(registry.activeRequests()).doesNotContain(assistantMessageId);
    }

    @Test
    void stopsTheReadingLoopWhenCancellationIsRequested() {
        reservationIsAvailable();
        when(client.supports(ProviderType.OLLAMA)).thenReturn(true);
        when(client.stream(any(), any(), any())).thenAnswer(call -> {
            BooleanSupplier cancelled = call.getArgument(2);
            assertThat(cancelled.getAsBoolean()).isFalse();
            service.cancel(userId, conversationId, assistantMessageId);
            assertThat(cancelled.getAsBoolean()).isTrue();
            return new ChatCompletionOutcome("part", null, null, null, true, "cancelled");
        });

        service.stream(userId, conversationId, "hi", listener);

        verify(store).markCancelling(userId, conversationId, assistantMessageId);
        assertThat(listener.cancelled).isNotNull();
    }

    @Test
    void refusesAProviderTypeWithoutAnAdapterInsteadOfFallingBack() {
        reservationIsAvailable();
        when(client.supports(ProviderType.OLLAMA)).thenReturn(false);

        assertThatThrownBy(() -> service.stream(userId, conversationId, "hi", listener))
                .isInstanceOf(UnsupportedProviderException.class);
        assertThat(listener.started).isNull();
    }

    @Test
    void failsInFlightRequestsOnShutdown() {
        when(store.failInFlightRequests("SERVER_SHUTDOWN")).thenReturn(2);

        service.failInFlightRequestsOnShutdown();

        verify(store).failInFlightRequests("SERVER_SHUTDOWN");
    }

    private void reservationIsAvailable() {
        when(store.reserve(userId, conversationId, "hi")).thenReturn(new ModelRequestReservation(
                userId,
                userMessageId,
                assistantMessageId,
                UUID.randomUUID(),
                new ChatCompletionCommand(ProviderType.OLLAMA, "http://127.0.0.1:11434", "qwen3:8b",
                        List.of(new ChatCompletionMessage("user", "hi"))),
                ProcessingLocation.LOCAL));
    }

    private static final class RecordingListener implements ModelStreamListener {
        private StartedEvent started;
        private final List<TokenEvent> tokens = new ArrayList<>();
        private UsageEvent usage;
        private CompletedEvent completed;
        private CancelledEvent cancelled;
        private StreamErrorEvent error;

        @Override
        public void onStarted(StartedEvent event) {
            started = event;
        }

        @Override
        public void onToken(TokenEvent event) {
            tokens.add(event);
        }

        @Override
        public void onUsage(UsageEvent event) {
            usage = event;
        }

        @Override
        public void onCompleted(CompletedEvent event) {
            completed = event;
        }

        @Override
        public void onCancelled(CancelledEvent event) {
            cancelled = event;
        }

        @Override
        public void onError(StreamErrorEvent event) {
            error = event;
        }
    }
}
