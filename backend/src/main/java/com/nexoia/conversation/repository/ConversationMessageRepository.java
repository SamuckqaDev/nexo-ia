package com.nexoia.conversation.repository;
import com.nexoia.conversation.model.ConversationMessage;
import java.util.List; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {
    List<ConversationMessage> findAllByConversationIdOrderBySequenceNumberAsc(UUID conversationId);
    long countByConversationId(UUID conversationId);
}
