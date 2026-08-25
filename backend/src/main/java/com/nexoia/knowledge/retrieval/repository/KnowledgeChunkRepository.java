package com.nexoia.knowledge.retrieval.repository;

import com.nexoia.knowledge.retrieval.model.KnowledgeChunk;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    List<KnowledgeChunk> findAllBySourceIdOrderByOrdinalAsc(UUID sourceId);

    void deleteAllBySourceId(UUID sourceId);

    @Query("""
            SELECT c FROM KnowledgeChunk c
            JOIN KnowledgeSource s ON s.id = c.sourceId
            JOIN KnowledgeVault v ON v.id = s.vaultId
            WHERE v.ownerId = :ownerId AND v.archived = false AND s.archived = false
              AND s.status = com.nexoia.knowledge.ingestion.model.SourceStatus.READY
              AND c.sourceId IN :sourceIds
            ORDER BY c.createdAt ASC
            """)
    List<KnowledgeChunk> findAuthorizedForGraph(
            @Param("ownerId") UUID ownerId,
            @Param("sourceIds") Collection<UUID> sourceIds,
            Limit limit);

    /**
     * The authorization boundary for retrieval: the join from chunk to source to vault filters by the
     * authorized owner set and {@code archived}/{@code status} before ranking, so a chunk under a vault the
     * caller may not access can never be ranked, let alone returned. {@code authorizedOwnerIds} is the
     * caller plus the Teams they belong to — defense in depth on top of the pre-authorized {@code vaultIds}.
     * A generic vector-store abstraction cannot express this join, which is why this query is hand-written
     * instead of using Spring AI's VectorStore. See D-026.
     */
    @Query("""
            SELECT c FROM KnowledgeChunk c
            JOIN KnowledgeSource s ON s.id = c.sourceId
            JOIN KnowledgeVault v ON v.id = s.vaultId
            WHERE v.ownerId IN :authorizedOwnerIds AND v.archived = false AND v.id IN :vaultIds
              AND s.archived = false AND s.status = com.nexoia.knowledge.ingestion.model.SourceStatus.READY
            ORDER BY cosine_distance(c.embedding, :queryVector) ASC
            """)
    List<KnowledgeChunk> findAuthorizedNearest(
            @Param("authorizedOwnerIds") Collection<UUID> authorizedOwnerIds,
            @Param("vaultIds") Collection<UUID> vaultIds,
            @Param("queryVector") float[] queryVector,
            Limit limit);
}
