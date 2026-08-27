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

import com.nexoia.audit.service.AuditService;
import com.nexoia.conversation.inference.config.ConversationContextProperties;
import com.nexoia.conversation.chat.model.ConversationMode;
import com.nexoia.conversation.inference.dto.ModelRequestExecutionPlan;
import com.nexoia.conversation.inference.dto.ModelRequestReservation;
import com.nexoia.conversation.inference.dto.event.CancelledEvent;
import com.nexoia.conversation.inference.dto.event.AgentStateEvent;
import com.nexoia.conversation.inference.dto.event.CompletedEvent;
import com.nexoia.conversation.inference.dto.event.StartedEvent;
import com.nexoia.conversation.inference.dto.event.StreamErrorEvent;
import com.nexoia.conversation.inference.dto.event.ThinkingEvent;
import com.nexoia.conversation.inference.dto.event.TokenEvent;
import com.nexoia.conversation.inference.dto.event.ToolCompletedEvent;
import com.nexoia.conversation.inference.dto.event.ToolStartedEvent;
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
    private ModelRequestExecutionPlanner executionPlanner;
    @Mock
    private ChatCompletionClient client;
    @Mock
    private AuditService audit;

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
        service = new ModelRequestService(
                store, executionPlanner, registry, audit, List.of(client), clock,
                new ConversationContextProperties(8000, 4));
    }

    @Test
    void emitsStartedThinkingTokensUsageAndCompletedForASuccessfulRequest() {
        reservationIsAvailable(true);
        when(client.supports(ProviderType.OLLAMA)).thenReturn(true);
        when(client.stream(any(), any(), any(), any())).thenAnswer(call -> {
            Consumer<String> onThinking = call.getArgument(1);
            Consumer<String> onToken = call.getArgument(2);
            onThinking.accept("Checking");
            onToken.accept("Hel");
            onToken.accept("lo");
            return new ChatCompletionOutcome("Hello", 20, 3, TokenSource.PROVIDER, false, "stop");
        });
        when(store.recordCompletion(eq(assistantMessageId), any(), anyLong(), any()))
                .thenReturn(Instant.parse("2026-08-18T12:00:01Z"));

        service.stream(userId, conversationId, "hi", true, listener);

        assertThat(listener.started).isNotNull();
        assertThat(listener.started.assistantMessageId()).isEqualTo(assistantMessageId);
        assertThat(listener.thinking).extracting(ThinkingEvent::content).containsExactly("Checking");
        assertThat(listener.tokens).extracting(TokenEvent::content).containsExactly("Hel", "lo");
        assertThat(listener.tokens).extracting(TokenEvent::index).containsExactly(0, 1);
        assertThat(listener.usage.inputTokens()).isEqualTo(20);
        assertThat(listener.usage.outputTokens()).isEqualTo(3);
        assertThat(listener.usage.totalTokens()).isEqualTo(23L);
        assertThat(listener.usage.contextTokensUsed()).isEqualTo(20);
        assertThat(listener.usage.contextTokenBudget()).isEqualTo(8000);
        assertThat(listener.usage.tokenSource()).isEqualTo(TokenSource.PROVIDER);
        assertThat(listener.completed.content()).isEqualTo("Hello");
        assertThat(listener.cancelled).isNull();
        assertThat(listener.error).isNull();
    }

    @Test
    void persistsThePartialAnswerAndEmitsCancelledWithoutCompleting() {
        reservationIsAvailable(false);
        when(client.supports(ProviderType.OLLAMA)).thenReturn(true);
        when(client.stream(any(), any(), any(), any())).thenAnswer(call -> {
            Consumer<String> onToken = call.getArgument(2);
            onToken.accept("Hel");
            return new ChatCompletionOutcome("Hel", null, null, null, true, "cancelled");
        });
        when(store.recordCancellation(eq(assistantMessageId), eq("Hel"), anyLong()))
                .thenReturn(Instant.parse("2026-08-18T12:00:02Z"));

        service.stream(userId, conversationId, "hi", false, listener);

        assertThat(listener.cancelled.content()).isEqualTo("Hel");
        assertThat(listener.completed).isNull();
        assertThat(listener.usage).isNull();
        verify(store, never()).recordCompletion(any(), any(), anyLong(), any());
    }

    @Test
    void discardsProviderThinkingWhenThePreferenceIsDisabled() {
        reservationIsAvailable(false);
        when(client.supports(ProviderType.OLLAMA)).thenReturn(true);
        when(client.stream(any(), any(), any(), any())).thenAnswer(call -> {
            Consumer<String> onThinking = call.getArgument(1);
            onThinking.accept("Provider trace that must not leave the backend");
            return new ChatCompletionOutcome("Answer", 5, 1, TokenSource.PROVIDER, false, "stop");
        });
        when(store.recordCompletion(eq(assistantMessageId), any(), anyLong(), any()))
                .thenReturn(Instant.parse("2026-08-18T12:00:01Z"));

        service.stream(userId, conversationId, "hi", false, listener);

        assertThat(listener.thinking).isEmpty();
        assertThat(listener.completed.content()).isEqualTo("Answer");
    }

    @Test
    void recordsAFailureAndEmitsASafeErrorWhenTheProviderBreaks() {
        reservationIsAvailable(false);
        when(client.supports(ProviderType.OLLAMA)).thenReturn(true);
        when(client.stream(any(), any(), any(), any()))
                .thenThrow(new ProviderStreamException(new IOException("connection refused to 10.0.0.9")));

        service.stream(userId, conversationId, "hi", false, listener);

        assertThat(listener.error.code()).isEqualTo("PROVIDER_STREAM_FAILED");
        assertThat(listener.error.message()).doesNotContain("10.0.0.9");
        assertThat(listener.completed).isNull();
        verify(store).recordFailure(eq(assistantMessageId), eq("PROVIDER_STREAM_FAILED"), anyLong());
    }

    @Test
    void persistsARequiredToolFailureAsFailedInsteadOfCompleted() {
        reservationIsAvailable(false);
        when(client.supports(ProviderType.OLLAMA)).thenReturn(true);
        when(client.stream(any(), any(), any(), any()))
                .thenReturn(new ChatCompletionOutcome(
                        "A ação obrigatória não foi concluída.",
                        30,
                        8,
                        TokenSource.PROVIDER,
                        false,
                        "required_tool_failed"));

        service.stream(userId, conversationId, "hi", false, listener);

        assertThat(listener.completed).isNull();
        assertThat(listener.error.code()).isEqualTo("REQUIRED_TOOL_FAILED");
        assertThat(listener.usage.totalTokens()).isEqualTo(38L);
        verify(store).recordFailure(
                eq(assistantMessageId),
                eq("REQUIRED_TOOL_FAILED"),
                eq("A ação obrigatória não foi concluída."),
                anyLong());
        verify(store, never()).recordCompletion(any(), any(), anyLong(), any());
    }

    @Test
    void releasesTheCancellationSignalAfterEveryOutcome() {
        reservationIsAvailable(false);
        when(client.supports(ProviderType.OLLAMA)).thenReturn(true);
        when(client.stream(any(), any(), any(), any()))
                .thenReturn(new ChatCompletionOutcome("ok", 1, 1, TokenSource.PROVIDER, false, "stop"));

        service.stream(userId, conversationId, "hi", false, listener);

        assertThat(registry.activeRequests()).doesNotContain(assistantMessageId);
    }

    @Test
    void stopsTheReadingLoopWhenCancellationIsRequested() {
        reservationIsAvailable(false);
        when(client.supports(ProviderType.OLLAMA)).thenReturn(true);
        when(client.stream(any(), any(), any(), any())).thenAnswer(call -> {
            BooleanSupplier cancelled = call.getArgument(3);
            assertThat(cancelled.getAsBoolean()).isFalse();
            service.cancel(userId, conversationId, assistantMessageId);
            assertThat(cancelled.getAsBoolean()).isTrue();
            return new ChatCompletionOutcome("part", null, null, null, true, "cancelled");
        });

        service.stream(userId, conversationId, "hi", false, listener);

        verify(store).markCancelling(userId, conversationId, assistantMessageId);
        assertThat(listener.cancelled).isNotNull();
    }

    @Test
    void refusesAProviderTypeWithoutAnAdapterInsteadOfFallingBack() {
        reservationIsAvailable(false);
        when(client.supports(ProviderType.OLLAMA)).thenReturn(false);

        assertThatThrownBy(() -> service.stream(userId, conversationId, "hi", false, listener))
                .isInstanceOf(UnsupportedProviderException.class);
        assertThat(listener.started).isNull();
    }

    @Test
    void failsInFlightRequestsOnShutdown() {
        when(store.failInFlightRequests("SERVER_SHUTDOWN")).thenReturn(2);

        service.failInFlightRequestsOnShutdown();

        verify(store).failInFlightRequests("SERVER_SHUTDOWN");
    }

    private void reservationIsAvailable(boolean thinkingEnabled) {
        when(executionPlanner.plan(
                userId, conversationId, "hi", ConversationMode.CHAT, thinkingEnabled))
                .thenReturn(new ModelRequestExecutionPlan(
                        ConversationMode.CHAT, "qwen3:8b", "hi", false, false));
        when(store.reserve(
                userId,
                conversationId,
                "hi",
                thinkingEnabled,
                List.of(),
                ConversationMode.CHAT,
                "qwen3:8b"))
                .thenReturn(new ModelRequestReservation(
                userId,
                userMessageId,
                assistantMessageId,
                UUID.randomUUID(),
                new ChatCompletionCommand(ProviderType.OLLAMA, "http://127.0.0.1:11434", "qwen3:8b",
                        List.of(new ChatCompletionMessage("user", "hi")), thinkingEnabled),
                ProcessingLocation.LOCAL,
                List.of()));
    }

    private static final class RecordingListener implements ModelStreamListener {
        private StartedEvent started;
        private final List<ThinkingEvent> thinking = new ArrayList<>();
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
        public void onThinking(ThinkingEvent event) {
            thinking.add(event);
        }

        @Override
        public void onAgentState(AgentStateEvent event) {}

        @Override
        public void onToolStarted(ToolStartedEvent event) {}

        @Override
        public void onToolCompleted(ToolCompletedEvent event) {}

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
