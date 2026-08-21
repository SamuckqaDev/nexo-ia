package com.nexoia.conversation.inference.service;

import com.nexoia.auth.user.exception.UserNotFoundException;
import com.nexoia.auth.user.repository.UserAccountRepository;
import com.nexoia.conversation.chat.exception.ConversationBusyException;
import com.nexoia.conversation.chat.exception.ConversationNotFoundException;
import com.nexoia.conversation.chat.model.Conversation;
import com.nexoia.conversation.chat.model.ConversationMessage;
import com.nexoia.conversation.chat.model.ConversationRole;
import com.nexoia.conversation.chat.model.MessageStatus;
import com.nexoia.conversation.chat.repository.ConversationMessageRepository;
import com.nexoia.conversation.chat.repository.ConversationRepository;
import com.nexoia.conversation.inference.dto.ModelRequestReservation;
import com.nexoia.conversation.inference.exception.ModelNotSelectedException;
import com.nexoia.conversation.inference.exception.ModelRequestNotFoundException;
import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.exception.ProviderConfigurationNotFoundException;
import com.nexoia.provider.model.ProcessingLocation;
import com.nexoia.provider.model.ProviderConfiguration;
import com.nexoia.provider.repository.ProviderConfigurationRepository;
import com.nexoia.provider.service.ProviderEndpointGuard;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The transactional half of a model request.
 *
 * <p>It is a separate bean on purpose. Each method here must run in its own short transaction, and a
 * self-invoked {@code @Transactional} method inside {@link ModelRequestService} would bypass the
 * Spring proxy and silently run without one.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRequestStore {

    private static final Set<MessageStatus> IN_FLIGHT =
            Set.of(MessageStatus.QUEUED, MessageStatus.STREAMING, MessageStatus.CANCELLING);

    private final ConversationRepository conversations;
    private final ConversationMessageRepository messages;
    private final ProviderConfigurationRepository providers;
    private final UserAccountRepository users;
    private final ConversationContextAssembler contextAssembler;
    private final ProviderEndpointGuard endpointGuard;
    private final Clock clock;

    /**
     * Appends the user message and reserves the assistant message, returning the values the
     * streaming stage needs. Holds the conversation write lock for the duration of this transaction
     * only.
     */
    @Transactional
    public ModelRequestReservation reserve(
            UUID userId, UUID conversationId, String content, boolean thinkingEnabled) {
        Conversation conversation = conversations.findOwnedForUpdate(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);

        if (!conversation.hasSelectedModel()) {
            throw new ModelNotSelectedException();
        }

        ProviderConfiguration provider = providers
                .findByIdAndUserId(conversation.getProviderConfigurationId(), userId)
                .orElseThrow(ProviderConfigurationNotFoundException::new);
        ProcessingLocation processingLocation =
                endpointGuard.verify(provider.getProviderType(), provider.getEndpoint());

        UUID correlationId = UUID.randomUUID();
        int sequenceNumber = messages.findHighestSequenceNumber(conversationId);

        ConversationMessage userMessage = messages.save(ConversationMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .sequenceNumber(sequenceNumber + 1)
                .role(ConversationRole.USER)
                .status(MessageStatus.COMPLETED)
                .content(content.trim())
                .correlationId(correlationId)
                .build());

        ConversationMessage assistantMessage = ConversationMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .sequenceNumber(sequenceNumber + 2)
                .role(ConversationRole.ASSISTANT)
                .status(MessageStatus.QUEUED)
                .content("")
                .providerConfigurationId(provider.getId())
                .model(conversation.getSelectedModel())
                .processingLocation(processingLocation)
                .correlationId(correlationId)
                .build();

        try {
            // Flushed here so the partial unique index rejects a second active request while the
            // caller can still be given a normal conflict response.
            messages.saveAndFlush(assistantMessage);
        } catch (DataIntegrityViolationException exception) {
            throw new ConversationBusyException();
        }

        return new ModelRequestReservation(
                userId,
                userMessage.getId(),
                assistantMessage.getId(),
                correlationId,
                new ChatCompletionCommand(
                        provider.getProviderType(),
                        provider.getEndpoint(),
                        conversation.getSelectedModel(),
                        contextAssembler.assemble(
                                conversationId,
                                users.findById(userId)
                                        .orElseThrow(UserNotFoundException::new)
                                        .getUsername()),
                        thinkingEnabled),
                processingLocation);
    }

    @Transactional
    public void markStreaming(UUID messageId) {
        messages.findById(messageId).ifPresent(ConversationMessage::markStreaming);
    }

    @Transactional
    public Instant recordCompletion(UUID messageId, ChatCompletionOutcome outcome, long latencyMs) {
        Instant completedAt = clock.instant();
        messages.findById(messageId).ifPresent(message -> message.complete(
                outcome.content(),
                outcome.inputTokens(),
                outcome.outputTokens(),
                outcome.tokenSource(),
                latencyMs,
                completedAt));

        return completedAt;
    }

    @Transactional
    public Instant recordCancellation(UUID messageId, String partialContent, long latencyMs) {
        Instant cancelledAt = clock.instant();
        messages.findById(messageId)
                .ifPresent(message -> message.cancel(partialContent, latencyMs, cancelledAt));

        return cancelledAt;
    }

    @Transactional
    public void recordFailure(UUID messageId, String failureCode, long latencyMs) {
        messages.findById(messageId).ifPresent(message ->
                message.fail(failureCode, message.getContent(), latencyMs, clock.instant()));
    }

    /**
     * Moves a running request to CANCELLING. The streaming stage writes the terminal state once the
     * reading loop stops.
     */
    @Transactional
    public void markCancelling(UUID userId, UUID conversationId, UUID messageId) {
        conversations.findByIdAndUserIdAndArchivedFalse(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
        ConversationMessage message = messages.findByIdAndConversationId(messageId, conversationId)
                .orElseThrow(ModelRequestNotFoundException::new);

        if (message.getStatus().isTerminal()) {
            throw new ModelRequestNotFoundException();
        }

        message.markCancelling();
    }

    @Transactional
    public int failInFlightRequests(String failureCode) {
        List<ConversationMessage> inFlight = messages.findAllByStatusIn(IN_FLIGHT);
        Instant failedAt = clock.instant();
        inFlight.forEach(message -> message.fail(failureCode, message.getContent(), 0L, failedAt));

        return inFlight.size();
    }
}
