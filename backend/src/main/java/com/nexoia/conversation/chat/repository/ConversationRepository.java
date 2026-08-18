package com.nexoia.conversation.chat.repository;

import com.nexoia.conversation.chat.model.Conversation;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findAllByUserIdAndArchivedFalseOrderByUpdatedAtDesc(UUID userId);

    Optional<Conversation> findByIdAndUserIdAndArchivedFalse(UUID id, UUID userId);

    /**
     * Locks the conversation row so concurrent writes to the same conversation are serialized.
     * Message appends, model selection, rename, and archive must use this method; readers must not,
     * so unrelated conversations never contend.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM Conversation c
            WHERE c.id = :conversationId AND c.userId = :userId AND c.archived = false
            """)
    Optional<Conversation> findOwnedForUpdate(
            @Param("conversationId") UUID conversationId,
            @Param("userId") UUID userId);
}
