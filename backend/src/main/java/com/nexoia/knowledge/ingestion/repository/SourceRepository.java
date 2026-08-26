package com.nexoia.knowledge.ingestion.repository;

import com.nexoia.knowledge.ingestion.model.KnowledgeSource;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SourceRepository extends JpaRepository<KnowledgeSource, UUID> {

    List<KnowledgeSource> findAllByVaultIdAndArchivedFalseOrderByCreatedAtDesc(UUID vaultId);

    Optional<KnowledgeSource> findByIdAndArchivedFalse(UUID id);

    Optional<KnowledgeSource> findByVaultIdAndContentHash(UUID vaultId, String contentHash);

    @Query("""
            SELECT s FROM KnowledgeSource s
            JOIN KnowledgeVault v ON v.id = s.vaultId
            WHERE v.ownerId IN :authorizedOwnerIds AND v.archived = false AND v.id IN :vaultIds
              AND s.archived = false
            ORDER BY s.updatedAt DESC
            """)
    List<KnowledgeSource> findAuthorizedForGraph(
            @Param("authorizedOwnerIds") Collection<UUID> authorizedOwnerIds,
            @Param("vaultIds") List<UUID> vaultIds,
            Limit limit);
}
