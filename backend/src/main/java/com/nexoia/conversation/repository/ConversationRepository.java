package com.nexoia.conversation.repository;
import com.nexoia.conversation.model.Conversation;
import java.util.List; import java.util.Optional; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findAllByUserIdAndArchivedFalseOrderByUpdatedAtDesc(UUID userId);
    Optional<Conversation> findByIdAndUserIdAndArchivedFalse(UUID id, UUID userId);
}
