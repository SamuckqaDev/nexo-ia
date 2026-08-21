package com.nexoia.knowledge.retrieval.repository;

import com.nexoia.knowledge.retrieval.model.KnowledgeChunk;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    List<KnowledgeChunk> findAllBySourceIdOrderByOrdinalAsc(UUID sourceId);

    void deleteAllBySourceId(UUID sourceId);
}
