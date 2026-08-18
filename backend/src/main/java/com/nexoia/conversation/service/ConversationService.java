package com.nexoia.conversation.service;

import com.nexoia.conversation.dto.ConversationMessageResponse;
import com.nexoia.conversation.dto.ConversationResponse;
import com.nexoia.conversation.dto.CreateConversationRequest;
import com.nexoia.conversation.dto.UpdateConversationModelRequest;
import com.nexoia.conversation.exception.ConversationNotFoundException;
import com.nexoia.conversation.model.Conversation;
import com.nexoia.conversation.model.ConversationMessage;
import com.nexoia.conversation.model.ConversationRole;
import com.nexoia.conversation.repository.ConversationMessageRepository;
import com.nexoia.conversation.repository.ConversationRepository;
import com.nexoia.provider.repository.ProviderConfigurationRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversations;
    private final ConversationMessageRepository messages;
    private final ProviderConfigurationRepository providers;

    @Transactional(readOnly = true)
    public List<ConversationResponse> list(UUID userId) { return conversations.findAllByUserIdAndArchivedFalseOrderByUpdatedAtDesc(userId).stream().map(this::conversationResponse).toList(); }

    @Transactional
    public ConversationResponse create(UUID userId, CreateConversationRequest request) {
        Conversation conversation = conversations.save(Conversation.builder().id(UUID.randomUUID()).userId(userId).title(request.title().trim()).archived(false).build());
        return conversationResponse(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationMessageResponse> messages(UUID userId, UUID conversationId) {
        ownedConversation(userId, conversationId);
        return messages.findAllByConversationIdOrderBySequenceNumberAsc(conversationId).stream().map(this::messageResponse).toList();
    }

    @Transactional
    public ConversationMessageResponse addUserMessage(UUID userId, UUID conversationId, String content) {
        ownedConversation(userId, conversationId);
        ConversationMessage message = messages.save(ConversationMessage.builder().id(UUID.randomUUID()).conversationId(conversationId)
                .sequenceNumber(Math.toIntExact(messages.countByConversationId(conversationId)) + 1).role(ConversationRole.USER).content(content.trim()).build());
        return messageResponse(message);
    }

    @Transactional
    public ConversationResponse selectModel(UUID userId, UUID conversationId, UpdateConversationModelRequest request) {
        Conversation conversation = ownedConversation(userId, conversationId);
        providers.findByIdAndUserId(request.providerConfigurationId(), userId)
                .orElseThrow(ConversationNotFoundException::new);
        conversation.selectModel(request.providerConfigurationId(), request.selectedModel().trim());
        return conversationResponse(conversation);
    }

    private Conversation ownedConversation(UUID userId, UUID conversationId) { return conversations.findByIdAndUserIdAndArchivedFalse(conversationId, userId).orElseThrow(ConversationNotFoundException::new); }
    private ConversationResponse conversationResponse(Conversation value) { return new ConversationResponse(value.getId(), value.getTitle(), value.getProviderConfigurationId(), value.getSelectedModel(), value.getCreatedAt(), value.getUpdatedAt()); }
    private ConversationMessageResponse messageResponse(ConversationMessage value) { return new ConversationMessageResponse(value.getId(), value.getRole(), value.getContent(), value.getCreatedAt()); }
}
