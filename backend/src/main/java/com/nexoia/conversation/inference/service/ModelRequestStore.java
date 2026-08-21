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
import com.nexoia.knowledge.embedding.exception.EmbeddingProviderUnavailableException;
import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.knowledge.retrieval.dto.RetrievalQuery;
import com.nexoia.knowledge.retrieval.dto.RetrievalResult;
import com.nexoia.knowledge.retrieval.service.RetrievalService;
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
    private final RetrievalService retrievalService;
    private final Clock clock;

    /**
     * Reserves without selecting any Knowledge Vault — equivalent to {@code knowledgeVaultIds =
     * List.of()}.
     */
    public ModelRequestReservation reserve(
            UUID userId, UUID conversationId, String content, boolean thinkingEnabled) {
        return reserve(userId, conversationId, content, thinkingEnabled, List.of());
    }

    /**
     * Appends the user message and reserves the assistant message, returning the values the
     * streaming stage needs. Holds the conversation write lock for the duration of this transaction
     * only.
     *
     * <p>Retrieval failure (an unavailable embedding provider) never fails the request — chat must
     * keep working with zero citations, per D-026. Only an explicit isolation boundary (an
     * unauthorized or archived vault) is allowed to silently produce no citations; any other
     * retrieval defect is a bug, not a reason to degrade silently, so only
     * {@link EmbeddingProviderUnavailableException} is caught here.
     */
    @Transactional
    public ModelRequestReservation reserve(
            UUID userId, UUID conversationId, String content, boolean thinkingEnabled,
            List<UUID> knowledgeVaultIds) {
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

        List<CitationResponse> citations = retrieve(userId, knowledgeVaultIds, content);

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
                                        .getUsername(),
                                citations),
                        thinkingEnabled),
                processingLocation,
                citations);
    }

    private List<CitationResponse> retrieve(UUID userId, List<UUID> knowledgeVaultIds, String content) {
        try {
            RetrievalResult result = retrievalService.retrieve(userId, new RetrievalQuery(knowledgeVaultIds, content));
            return result.citations();
        } catch (EmbeddingProviderUnavailableException exception) {
            log.warn("[NEXO-BACK][KNOWLEDGE] Embedding provider unavailable; continuing without citations userId={}",
                    userId);
            return List.of();
        }
    }

    @Transactional
    public void markStreaming(UUID messageId) {
        messages.findById(messageId).ifPresent(ConversationMessage::markStreaming);
    }

    @Transactional
    public Instant recordCompletion(
            UUID messageId, ChatCompletionOutcome outcome, long latencyMs, List<CitationResponse> citations) {
        Instant completedAt = clock.instant();
        messages.findById(messageId).ifPresent(message -> message.complete(
                outcome.content(),
                outcome.inputTokens(),
                outcome.outputTokens(),
                outcome.tokenSource(),
                latencyMs,
                completedAt,
                citations));

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
