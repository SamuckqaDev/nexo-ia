package com.nexoia.conversation.inference.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditOutcome;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.conversation.chat.model.ConversationMode;
import com.nexoia.conversation.inference.config.ConversationContextProperties;
import com.nexoia.conversation.inference.dto.ModelRequestReservation;
import com.nexoia.conversation.inference.dto.event.AgentStateEvent;
import com.nexoia.conversation.inference.dto.event.AgentPlanStepEvent;
import com.nexoia.conversation.inference.dto.event.CancelledEvent;
import com.nexoia.conversation.inference.dto.event.CompletedEvent;
import com.nexoia.conversation.inference.dto.event.PlanUpdatedEvent;
import com.nexoia.conversation.inference.dto.event.StartedEvent;
import com.nexoia.conversation.inference.dto.event.StreamErrorEvent;
import com.nexoia.conversation.inference.dto.event.ThinkingEvent;
import com.nexoia.conversation.inference.dto.event.TokenEvent;
import com.nexoia.conversation.inference.dto.event.ToolCompletedEvent;
import com.nexoia.conversation.inference.dto.event.ToolStartedEvent;
import com.nexoia.conversation.inference.dto.event.UsageEvent;
import com.nexoia.conversation.inference.exception.UnsupportedProviderException;
import com.nexoia.conversation.inference.model.AgentState;
import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.provider.dto.AgentPlanUpdate;
import com.nexoia.provider.dto.AgentPlanUpdateObserver;
import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.dto.ToolExecutionObserver;
import com.nexoia.provider.dto.ToolExecutionStarted;
import com.nexoia.provider.service.ChatCompletionClient;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
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
    private final AuditService audit;
    private final List<ChatCompletionClient> completionClients;
    private final Clock clock;
    private final ConversationContextProperties contextProperties;

    /**
     * Streams a model answer, reporting progress through the listener.
     *
     * <p>Every validation failure is raised by the reservation, before the first event is emitted, so
     * the transport can still answer with a normal error status.
     */
    public void stream(
            UUID userId,
            UUID conversationId,
            String content,
            boolean thinkingEnabled,
            ModelStreamListener listener) {
        run(begin(userId, conversationId, content, thinkingEnabled), listener);
    }

    /**
     * Validates the request and reserves its messages, without selecting any Knowledge Vault.
     */
    public ModelRequestReservation begin(
            UUID userId, UUID conversationId, String content, boolean thinkingEnabled) {
        return begin(
                userId, conversationId, content, thinkingEnabled,
                List.of(), ConversationMode.CHAT);
    }

    /**
     * Validates the request and reserves its messages.
     *
     * <p>Called before a streaming transport opens its response, so a missing conversation, an
     * unselected model, a busy conversation, or an unsupported provider is still reported as an
     * ordinary error status rather than as an event on an already-committed stream.
     */
    public ModelRequestReservation begin(
            UUID userId, UUID conversationId, String content, boolean thinkingEnabled,
            List<UUID> knowledgeVaultIds) {
        return begin(
                userId, conversationId, content, thinkingEnabled,
                knowledgeVaultIds, ConversationMode.CHAT);
    }

    public ModelRequestReservation begin(
            UUID userId,
            UUID conversationId,
            String content,
            boolean thinkingEnabled,
            List<UUID> knowledgeVaultIds,
            ConversationMode mode) {
        ModelRequestReservation reservation =
                store.reserve(
                        userId, conversationId, content, thinkingEnabled, knowledgeVaultIds, mode);
        clientFor(reservation.command());

        return reservation;
    }

    /** Streams the reserved request to a terminal state. */
    public void run(ModelRequestReservation reservation, ModelStreamListener listener) {
        ChatCompletionClient client = clientFor(reservation.command());
        UUID messageId = reservation.assistantMessageId();
        long startedAt = clock.millis();

        listener.onStarted(new StartedEvent(
                reservation.userMessageId(),
                messageId,
                reservation.correlationId(),
                reservation.command().model(),
                reservation.processingLocation()));

        if (reservation.command().mode() == ConversationMode.AGENT) {
            listener.onAgentState(new AgentStateEvent(AgentState.PLANNING, clock.instant()));
        }
        registry.register(messageId);
        store.markStreaming(messageId);
        if (reservation.command().mode() == ConversationMode.AGENT) {
            listener.onAgentState(new AgentStateEvent(AgentState.RUNNING, clock.instant()));
        }
        auditModelRequest(reservation, AuditAction.MODEL_REQUEST_STARTED, AuditOutcome.SUCCESS,
                reservation.command().model());
        AtomicInteger thinkingIndex = new AtomicInteger();
        AtomicInteger tokenIndex = new AtomicInteger();

        try {
            ChatCompletionCommand executableCommand = reservation.command()
                    .withExecutionObservers(
                            toolObserver(reservation, listener),
                            planObserver(reservation, listener));
            ChatCompletionOutcome outcome = client.stream(
                    executableCommand,
                    delta -> {
                        if (executableCommand.thinkingEnabled()) {
                            listener.onThinking(new ThinkingEvent(
                                    delta, thinkingIndex.getAndIncrement()));
                        }
                    },
                    delta -> listener.onToken(new TokenEvent(delta, tokenIndex.getAndIncrement())),
                    () -> registry.isCancellationRequested(messageId));
            long latencyMs = clock.millis() - startedAt;

            if (outcome.cancelled()) {
                Instant cancelledAt = store.recordCancellation(messageId, outcome.content(), latencyMs);
                auditModelRequest(reservation, AuditAction.MODEL_REQUEST_CANCELLED, AuditOutcome.SUCCESS, null);
                emitAgentState(reservation, listener, AgentState.CANCELLED);
                listener.onCancelled(new CancelledEvent(messageId, outcome.content(), cancelledAt));
                return;
            }

            List<CitationResponse> citations = mergeCitations(
                    reservation.citations(), outcome.toolExecutions());
            if (reservation.command().mode() == ConversationMode.AGENT) {
                store.markVerifying(messageId);
                listener.onAgentState(new AgentStateEvent(AgentState.VERIFYING, clock.instant()));
            }
            Instant completedAt =
                    store.recordCompletion(messageId, outcome, latencyMs, citations);
            auditModelRequest(reservation, AuditAction.MODEL_REQUEST_COMPLETED, AuditOutcome.SUCCESS, null);
            listener.onUsage(new UsageEvent(
                    outcome.inputTokens(),
                    outcome.outputTokens(),
                    totalTokens(outcome),
                    outcome.inputTokens(),
                    contextProperties.tokenBudget(),
                    outcome.tokenSource(),
                    latencyMs));
            emitAgentState(reservation, listener, AgentState.COMPLETED);
            listener.onCompleted(new CompletedEvent(
                    messageId, outcome.content(), completedAt, citations));
        } catch (RuntimeException exception) {
            log.warn("[NEXO-BACK][INFERENCE] Model request failed correlationId={} reason={}",
                    reservation.correlationId(), exception.getClass().getSimpleName());
            store.recordFailure(messageId, STREAM_FAILURE, clock.millis() - startedAt);
            auditModelRequest(reservation, AuditAction.MODEL_REQUEST_FAILED, AuditOutcome.FAILURE, STREAM_FAILURE);
            emitAgentState(reservation, listener, AgentState.FAILED);
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

    private void auditModelRequest(
            ModelRequestReservation reservation, AuditAction action, AuditOutcome outcome, String detail) {
        audit.record(new RecordAuditCommand(
                action, outcome, reservation.userId(), null, AuditTargetType.MESSAGE,
                reservation.assistantMessageId(), reservation.correlationId(), detail));
    }

    private ChatCompletionClient clientFor(ChatCompletionCommand command) {
        return completionClients.stream()
                .filter(client -> client.supports(command.providerType()))
                .findFirst()
                .orElseThrow(UnsupportedProviderException::new);
    }

    private ToolExecutionObserver toolObserver(
            ModelRequestReservation reservation,
            ModelStreamListener listener) {
        return new ToolExecutionObserver() {
            @Override
            public void onStarted(ToolExecutionStarted event) {
                store.recordToolStarted(
                        reservation.assistantMessageId(), reservation.correlationId(), event);
                listener.onAgentState(new AgentStateEvent(AgentState.RUNNING, clock.instant()));
                listener.onToolStarted(new ToolStartedEvent(
                        event.executionId(), event.toolName(), event.startedAt()));
            }

            @Override
            public void onCompleted(ToolExecutionEvidence event) {
                store.recordToolCompleted(event);
                listener.onToolCompleted(new ToolCompletedEvent(
                        event.executionId(),
                        event.toolName(),
                        event.status(),
                        event.durationMs(),
                        event.citations(),
                        event.completedAt()));
            }
        };
    }

    private AgentPlanUpdateObserver planObserver(
            ModelRequestReservation reservation,
            ModelStreamListener listener) {
        return update -> {
            store.recordPlanUpdated(reservation.assistantMessageId(), update);
            listener.onPlanUpdated(planUpdatedEvent(update));
        };
    }

    private PlanUpdatedEvent planUpdatedEvent(AgentPlanUpdate update) {
        return new PlanUpdatedEvent(
                update.revision(),
                update.explanation(),
                update.steps().stream()
                        .map(step -> new AgentPlanStepEvent(step.step(), step.status()))
                        .toList(),
                update.updatedAt());
    }

    private List<CitationResponse> mergeCitations(
            List<CitationResponse> deterministic,
            List<ToolExecutionEvidence> toolExecutions) {
        return Stream.concat(
                        deterministic.stream(),
                        toolExecutions.stream().flatMap(event -> event.citations().stream()))
                .distinct()
                .toList();
    }

    private void emitAgentState(
            ModelRequestReservation reservation,
            ModelStreamListener listener,
            AgentState state) {
        if (reservation.command().mode() == ConversationMode.AGENT) {
            listener.onAgentState(new AgentStateEvent(state, clock.instant()));
        }
    }

    private Long totalTokens(ChatCompletionOutcome outcome) {
        if (outcome.inputTokens() == null && outcome.outputTokens() == null) {
            return null;
        }

        return (outcome.inputTokens() == null ? 0L : outcome.inputTokens().longValue())
                + (outcome.outputTokens() == null ? 0L : outcome.outputTokens().longValue());
    }
}
