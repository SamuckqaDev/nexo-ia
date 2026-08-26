package com.nexoia.media.image.repository;

import com.nexoia.media.image.model.ImageGenerationJob;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageGenerationJobRepository extends JpaRepository<ImageGenerationJob, UUID> {

    List<ImageGenerationJob> findAllByUserIdAndConversationIdOrderByCreatedAtDesc(
            UUID userId,
            UUID conversationId);

    Optional<ImageGenerationJob> findByIdAndUserId(UUID id, UUID userId);
}
