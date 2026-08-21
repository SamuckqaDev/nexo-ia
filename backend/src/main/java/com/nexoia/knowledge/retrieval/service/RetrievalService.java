package com.nexoia.knowledge.retrieval.service;

import com.nexoia.knowledge.embedding.service.EmbeddingService;
import com.nexoia.knowledge.ingestion.model.KnowledgeSource;
import com.nexoia.knowledge.ingestion.repository.SourceRepository;
import com.nexoia.knowledge.retrieval.config.KnowledgeRetrievalProperties;
import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.knowledge.retrieval.dto.RetrievalQuery;
import com.nexoia.knowledge.retrieval.dto.RetrievalResult;
import com.nexoia.knowledge.retrieval.exception.RetrievalBudgetExceededException;
import com.nexoia.knowledge.retrieval.model.KnowledgeChunk;
import com.nexoia.knowledge.retrieval.repository.KnowledgeChunkRepository;
import com.nexoia.knowledge.vault.model.KnowledgeVault;
import com.nexoia.knowledge.vault.model.VaultScope;
import com.nexoia.knowledge.vault.repository.VaultRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retrieves authorized, ranked chunks for a query. The authorization join lives in
 * {@link KnowledgeChunkRepository#findAuthorizedNearest}; this service only ranks, filters by minimum
 * score, and caps by the retrieval token budget — a result from an unauthorized source can never reach
 * this code, let alone a model request. See D-026.
 */
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private static final int MAX_VAULTS = 8;
    private static final int MAX_EXCERPT_CHARACTERS = 600;
    private static final int ESTIMATED_CHARACTERS_PER_TOKEN = 4;

    /** Scopes retrieval can actually authorize today. See {@code VaultService.UNSUPPORTED_SCOPES}. */
    private static final Set<VaultScope> RETRIEVABLE_SCOPES = EnumSet.of(VaultScope.PERSONAL, VaultScope.WORKSPACE);

    private final VaultRepository vaults;
    private final SourceRepository sources;
    private final KnowledgeChunkRepository chunks;
    private final EmbeddingService embeddingService;
    private final KnowledgeRetrievalProperties properties;

    @Transactional(readOnly = true)
    public RetrievalResult retrieve(UUID ownerId, RetrievalQuery query) {
        if (query.vaultIds().size() > MAX_VAULTS) {
            throw new RetrievalBudgetExceededException();
        }
        if (query.vaultIds().isEmpty()) {
            return RetrievalResult.empty();
        }

        List<KnowledgeVault> authorizedVaults = vaults
                .findAllByOwnerIdAndArchivedFalseAndIdIn(ownerId, query.vaultIds()).stream()
                .filter(vault -> RETRIEVABLE_SCOPES.contains(vault.getScope()))
                .toList();
        if (authorizedVaults.isEmpty()) {
            return RetrievalResult.empty();
        }

        List<UUID> authorizedVaultIds = authorizedVaults.stream().map(KnowledgeVault::getId).toList();
        Map<UUID, String> vaultNames = authorizedVaults.stream()
                .collect(Collectors.toMap(KnowledgeVault::getId, KnowledgeVault::getName));

        float[] queryVector = embeddingService.embed(ownerId, List.of(query.text())).embeddings().getFirst();

        List<KnowledgeChunk> nearest = chunks.findAuthorizedNearest(
                ownerId, authorizedVaultIds, queryVector, Limit.of(properties.topK()));
        if (nearest.isEmpty()) {
            return RetrievalResult.empty();
        }

        Map<UUID, KnowledgeSource> sourcesById = sources
                .findAllById(nearest.stream().map(KnowledgeChunk::getSourceId).distinct().toList()).stream()
                .collect(Collectors.toMap(KnowledgeSource::getId, Function.identity()));

        List<ScoredChunk> scored = nearest.stream()
                .map(chunk -> new ScoredChunk(chunk, cosineSimilarity(queryVector, chunk.getEmbedding())))
                .filter(candidate -> candidate.score() >= properties.minimumScore())
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .toList();

        return new RetrievalResult(citations(scored, sourcesById, vaultNames));
    }

    private List<CitationResponse> citations(
            List<ScoredChunk> scored, Map<UUID, KnowledgeSource> sourcesById, Map<UUID, String> vaultNames) {
        List<CitationResponse> citations = new ArrayList<>();
        int remainingTokens = properties.contextTokenBudget();

        for (ScoredChunk candidate : scored) {
            KnowledgeSource source = sourcesById.get(candidate.chunk().getSourceId());
            if (source == null) {
                continue;
            }
            String excerpt = boundedExcerpt(candidate.chunk().getContent());
            int excerptTokens = Math.ceilDiv(excerpt.length(), ESTIMATED_CHARACTERS_PER_TOKEN);
            if (excerptTokens > remainingTokens) {
                break;
            }
            remainingTokens -= excerptTokens;
            citations.add(new CitationResponse(
                    vaultNames.get(source.getVaultId()), source.getDisplayName(),
                    candidate.chunk().getOrdinal(), excerpt, candidate.score()));
        }

        return citations;
    }

    private String boundedExcerpt(String content) {
        return content.length() > MAX_EXCERPT_CHARACTERS ? content.substring(0, MAX_EXCERPT_CHARACTERS) : content;
    }

    private double cosineSimilarity(float[] left, float[] right) {
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }

        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private record ScoredChunk(KnowledgeChunk chunk, double score) {
    }
}
