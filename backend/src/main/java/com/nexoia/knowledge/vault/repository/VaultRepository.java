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

    /** Vaults among {@code ids} owned by any of {@code ownerIds} — the caller plus their Teams. */
    List<KnowledgeVault> findAllByOwnerIdInAndArchivedFalseAndIdIn(Iterable<UUID> ownerIds, Iterable<UUID> ids);

    /** Every non-archived Vault owned by any of {@code ownerIds} — the caller's own plus their Teams'. */
    List<KnowledgeVault> findAllByOwnerIdInAndArchivedFalseOrderByUpdatedAtDesc(Iterable<UUID> ownerIds);
}
