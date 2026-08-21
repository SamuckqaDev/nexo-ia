package com.nexoia.knowledge.vault.repository;

import com.nexoia.knowledge.vault.model.KnowledgeVault;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VaultRepository extends JpaRepository<KnowledgeVault, UUID> {

    List<KnowledgeVault> findAllByOwnerIdAndArchivedFalseOrderByUpdatedAtDesc(UUID ownerId);

    List<KnowledgeVault> findAllByOwnerIdAndArchivedFalseOrderByUpdatedAtDesc(UUID ownerId, Limit limit);

    Optional<KnowledgeVault> findByIdAndOwnerIdAndArchivedFalse(UUID id, UUID ownerId);

    List<KnowledgeVault> findAllByOwnerIdAndArchivedFalseAndIdIn(UUID ownerId, Iterable<UUID> ids);
}
