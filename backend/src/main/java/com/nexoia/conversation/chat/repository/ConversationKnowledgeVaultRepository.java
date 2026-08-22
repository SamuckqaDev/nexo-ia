package com.nexoia.conversation.chat.repository;

import com.nexoia.conversation.chat.model.ConversationKnowledgeVault;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationKnowledgeVaultRepository
        extends JpaRepository<ConversationKnowledgeVault, UUID> {

    List<ConversationKnowledgeVault> findAllByConversationIdOrderBySelectedAtAsc(UUID conversationId);

    List<ConversationKnowledgeVault> findAllByConversationIdIn(Collection<UUID> conversationIds);

    void deleteAllByConversationId(UUID conversationId);
}
