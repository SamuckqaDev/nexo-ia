package com.nexoia.knowledge.ingestion.repository;

import com.nexoia.knowledge.ingestion.model.KnowledgeSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRepository extends JpaRepository<KnowledgeSource, UUID> {

    List<KnowledgeSource> findAllByVaultIdAndArchivedFalseOrderByCreatedAtDesc(UUID vaultId);

    Optional<KnowledgeSource> findByIdAndVaultIdAndArchivedFalse(UUID id, UUID vaultId);

    Optional<KnowledgeSource> findByVaultIdAndContentHash(UUID vaultId, String contentHash);
}
