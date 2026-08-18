package com.nexoia.conversation.inference.service;

import com.nexoia.conversation.inference.dto.ModelRequestReservation;
import com.nexoia.conversation.inference.dto.event.CancelledEvent;
import com.nexoia.conversation.inference.dto.event.CompletedEvent;
import com.nexoia.conversation.inference.dto.event.StartedEvent;
import com.nexoia.conversation.inference.dto.event.StreamErrorEvent;
import com.nexoia.conversation.inference.dto.event.TokenEvent;
import com.nexoia.conversation.inference.dto.event.UsageEvent;
import com.nexoia.conversation.inference.exception.UnsupportedProviderException;
import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.service.ChatCompletionClient;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Runs one model request from reservation to a terminal state.
 *
 * <p>Persistence happens in {@link ModelRequestStore}, in short transactions before and after the
 * stream. A generation can last minutes, so no database connection or conversation row lock is held
 * while tokens are arriving.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRequestService {

    private static final String STREAM_FAILURE = "PROVIDER_STREAM_FAILED";
    private static final String SHUTDOWN_FAILURE = "SERVER_SHUTDOWN";

    private final ModelRequestStore store;
    private final ModelRequestRegistry registry;
    private final List<ChatCompletionClient> completionClients;
    private final Clock clock;

    /**
     * Streams a model answer, reporting progress through the listener.
     *
     * <p>Every validation failure is raised by the reservation, before the first event is emitted, so
     * the transport can still answer with a normal error status.
     */
    public void stream(UUID userId, UUID conversationId, String content, ModelStreamListener listener) {
        ModelRequestReservation reservation = store.reserve(userId, conversationId, content);
        ChatCompletionClient client = clientFor(reservation.command());
        UUID messageId = reservation.assistantMessageId();
        long startedAt = clock.millis();

        listener.onStarted(new StartedEvent(
                reservation.userMessageId(),
                messageId,
                reservation.correlationId(),
                reservation.command().model(),
                reservation.processingLocation()));

        registry.register(messageId);
        store.markStreaming(messageId);
        AtomicInteger index = new AtomicInteger();

        try {
            ChatCompletionOutcome outcome = client.stream(
                    reservation.command(),
                    delta -> listener.onToken(new TokenEvent(delta, index.getAndIncrement())),
                    () -> registry.isCancellationRequested(messageId));
            long latencyMs = clock.millis() - startedAt;

            if (outcome.cancelled()) {
                Instant cancelledAt = store.recordCancellation(messageId, outcome.content(), latencyMs);
                listener.onCancelled(new CancelledEvent(messageId, outcome.content(), cancelledAt));
                return;
            }

            Instant completedAt = store.recordCompletion(messageId, outcome, latencyMs);
            listener.onUsage(new UsageEvent(
                    outcome.inputTokens(), outcome.outputTokens(), outcome.tokenSource(), latencyMs));
            listener.onCompleted(new CompletedEvent(messageId, outcome.content(), completedAt));
        } catch (RuntimeException exception) {
            log.warn("[NEXO-BACK][INFERENCE] Model request failed correlationId={} reason={}",
                    reservation.correlationId(), exception.getClass().getSimpleName());
            store.recordFailure(messageId, STREAM_FAILURE, clock.millis() - startedAt);
            listener.onError(new StreamErrorEvent(messageId, STREAM_FAILURE,
                    "The model provider could not complete this request"));
        } finally {
            registry.release(messageId);
        }
    }

    /**
     * Marks a running request for cancellation. The reading loop observes the signal, stops, and the
     * streaming stage persists the partial answer under the CANCELLED status.
     */
    public void cancel(UUID userId, UUID conversationId, UUID messageId) {
        store.markCancelling(userId, conversationId, messageId);
        registry.requestCancellation(messageId);
    }

    /**
     * Fails every request still in flight when the application stops, so a generation interrupted by
     * a restart is never reopened as if it had completed.
     */
    @PreDestroy
    public void failInFlightRequestsOnShutdown() {
        registry.activeRequests().forEach(registry::requestCancellation);
        int failed = store.failInFlightRequests(SHUTDOWN_FAILURE);

        if (failed > 0) {
            log.warn("[NEXO-BACK][INFERENCE] Failed {} in-flight model requests during shutdown", failed);
        }
    }

    private ChatCompletionClient clientFor(ChatCompletionCommand command) {
        return completionClients.stream()
                .filter(client -> client.supports(command.providerType()))
                .findFirst()
                .orElseThrow(UnsupportedProviderException::new);
    }
}
