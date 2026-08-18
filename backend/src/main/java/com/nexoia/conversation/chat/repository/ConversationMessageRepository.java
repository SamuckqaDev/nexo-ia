package com.nexoia.conversation.chat.repository;

import com.nexoia.conversation.chat.model.ConversationMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    List<ConversationMessage> findAllByConversationIdOrderBySequenceNumberAsc(UUID conversationId);

    /**
     * Reads the current highest sequence number. Callers must already hold the conversation write
     * lock, because the returned value is only stable inside that lock.
     */
    @Query("""
            SELECT COALESCE(MAX(m.sequenceNumber), 0) FROM ConversationMessage m
            WHERE m.conversationId = :conversationId
            """)
    int findHighestSequenceNumber(@Param("conversationId") UUID conversationId);
}
