package com.nexoia.workspace.repository;

import com.nexoia.workspace.model.Workspace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    List<Workspace> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Optional<Workspace> findByIdAndOwnerId(UUID id, UUID ownerId);
}
