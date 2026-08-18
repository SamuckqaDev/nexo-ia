package com.nexoia.conversation.chat.service;

import com.nexoia.conversation.chat.dto.ConversationMessageResponse;
import com.nexoia.conversation.chat.dto.ConversationResponse;
import com.nexoia.conversation.chat.dto.CreateConversationRequest;
import com.nexoia.conversation.chat.dto.RenameConversationRequest;
import com.nexoia.conversation.chat.dto.UpdateConversationModelRequest;
import com.nexoia.conversation.chat.exception.ConversationNotFoundException;
import com.nexoia.conversation.chat.model.Conversation;
import com.nexoia.conversation.chat.model.ConversationMessage;
import com.nexoia.conversation.chat.model.ConversationRole;
import com.nexoia.conversation.chat.model.MessageStatus;
import com.nexoia.conversation.chat.repository.ConversationMessageRepository;
import com.nexoia.conversation.chat.repository.ConversationRepository;
import com.nexoia.provider.exception.ProviderConfigurationNotFoundException;
import com.nexoia.provider.repository.ProviderConfigurationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversations;
    private final ConversationMessageRepository messages;
    private final ProviderConfigurationRepository providers;

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(UUID userId) {
        return conversations.findAllByUserIdAndArchivedFalseOrderByUpdatedAtDesc(userId).stream()
                .map(this::conversationResponse)
                .toList();
    }

    @Transactional
    public ConversationResponse create(UUID userId, CreateConversationRequest request) {
        Conversation conversation = conversations.save(Conversation.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .title(request.title().trim())
                .archived(false)
                .build());

        return conversationResponse(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationMessageResponse> messages(UUID userId, UUID conversationId) {
        readableConversation(userId, conversationId);

        return messages.findAllByConversationIdOrderBySequenceNumberAsc(conversationId).stream()
                .map(this::messageResponse)
                .toList();
    }

    @Transactional
    public ConversationMessageResponse addUserMessage(UUID userId, UUID conversationId, String content) {
        writableConversation(userId, conversationId);
        int sequenceNumber = messages.findHighestSequenceNumber(conversationId) + 1;

        ConversationMessage message = messages.save(ConversationMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .sequenceNumber(sequenceNumber)
                .role(ConversationRole.USER)
                .status(MessageStatus.COMPLETED)
                .content(content.trim())
                .build());

        return messageResponse(message);
    }

    @Transactional
    public ConversationResponse rename(UUID userId, UUID conversationId, RenameConversationRequest request) {
        Conversation conversation = writableConversation(userId, conversationId);
        conversation.rename(request.title().trim());

        return conversationResponse(conversation);
    }

    @Transactional
    public void archive(UUID userId, UUID conversationId) {
        writableConversation(userId, conversationId).archive();
    }

    @Transactional
    public ConversationResponse selectModel(
            UUID userId, UUID conversationId, UpdateConversationModelRequest request) {
        Conversation conversation = writableConversation(userId, conversationId);
        providers.findByIdAndUserId(request.providerConfigurationId(), userId)
                .orElseThrow(ProviderConfigurationNotFoundException::new);
        conversation.selectModel(request.providerConfigurationId(), request.selectedModel().trim());

        return conversationResponse(conversation);
    }

    private Conversation readableConversation(UUID userId, UUID conversationId) {
        return conversations.findByIdAndUserIdAndArchivedFalse(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
    }

    private Conversation writableConversation(UUID userId, UUID conversationId) {
        return conversations.findOwnedForUpdate(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
    }

    private ConversationResponse conversationResponse(Conversation value) {
        return new ConversationResponse(
                value.getId(),
                value.getTitle(),
                value.getProviderConfigurationId(),
                value.getSelectedModel(),
                value.getCreatedAt(),
                value.getUpdatedAt());
    }

    private ConversationMessageResponse messageResponse(ConversationMessage value) {
        return new ConversationMessageResponse(
                value.getId(),
                value.getRole(),
                value.getStatus(),
                value.getContent(),
                value.getModel(),
                value.getInputTokens(),
                value.getOutputTokens(),
                value.getTokenSource(),
                value.getLatencyMs(),
                value.getProcessingLocation(),
                value.getFailureCode(),
                value.getCreatedAt(),
                value.getCompletedAt());
    }
}
