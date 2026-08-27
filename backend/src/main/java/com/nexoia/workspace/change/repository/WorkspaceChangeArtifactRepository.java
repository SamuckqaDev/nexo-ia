package com.nexoia.workspace.change.repository;

import com.nexoia.workspace.change.model.WorkspaceChangeArtifact;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceChangeArtifactRepository extends JpaRepository<WorkspaceChangeArtifact, UUID> {

    List<WorkspaceChangeArtifact> findAllByUserIdAndConversationIdOrderByCreatedAtDesc(
            UUID userId, UUID conversationId);

    Optional<WorkspaceChangeArtifact> findByIdAndUserId(UUID id, UUID userId);
}
