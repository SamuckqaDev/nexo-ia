package com.nexoia.conversation.chat.repository;

import com.nexoia.conversation.chat.model.ConversationMessage;
import com.nexoia.conversation.chat.model.MessageStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

    /**
     * Returns the ordered history usable as model context. FAILED messages are excluded because a
     * failed generation never became part of the conversation.
     */
    @Query("""
            SELECT m FROM ConversationMessage m
            WHERE m.conversationId = :conversationId AND m.status IN :statuses
            ORDER BY m.sequenceNumber ASC
            """)
    List<ConversationMessage> findContextHistory(
            @Param("conversationId") UUID conversationId,
            @Param("statuses") Collection<MessageStatus> statuses);

    Optional<ConversationMessage> findByIdAndConversationId(UUID id, UUID conversationId);

    List<ConversationMessage> findAllByStatusIn(Collection<MessageStatus> statuses);
}
