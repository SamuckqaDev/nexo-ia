package com.nexoia.knowledge.graph.service;

import com.nexoia.knowledge.graph.dto.KnowledgeGraphEdgeResponse;
import com.nexoia.knowledge.graph.dto.KnowledgeGraphNodeKind;
import com.nexoia.knowledge.graph.dto.KnowledgeGraphNodeResponse;
import com.nexoia.knowledge.graph.dto.KnowledgeGraphRelation;
import com.nexoia.knowledge.graph.dto.KnowledgeGraphResponse;
import com.nexoia.knowledge.ingestion.model.KnowledgeSource;
import com.nexoia.knowledge.ingestion.repository.SourceRepository;
import com.nexoia.knowledge.retrieval.model.KnowledgeChunk;
import com.nexoia.knowledge.retrieval.repository.KnowledgeChunkRepository;
import com.nexoia.knowledge.vault.model.KnowledgeVault;
import com.nexoia.knowledge.vault.model.VaultOwnerType;
import com.nexoia.knowledge.vault.repository.VaultRepository;
import com.nexoia.team.model.Team;
import com.nexoia.team.repository.TeamRepository;
import com.nexoia.team.service.TeamMembershipService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KnowledgeGraphService {

    private static final int MAX_VAULTS = 24;
    private static final int MAX_SOURCES = 96;
    private static final int MAX_CHUNKS = 160;
    private static final int MAX_SEMANTIC_EDGES = 120;
    private static final int MAX_SEMANTIC_EDGES_PER_CHUNK = 3;
    private static final int MAX_EXCERPT_CHARACTERS = 220;
    private static final double MIN_SEMANTIC_SIMILARITY = 0.58;

    private final VaultRepository vaults;
    private final SourceRepository sources;
    private final KnowledgeChunkRepository chunks;
    private final TeamMembershipService teamMembershipService;
    private final TeamRepository teams;

    @Transactional(readOnly = true)
    public KnowledgeGraphResponse graph(UUID ownerId) {
        List<UUID> authorizedOwnerIds = teamMembershipService.accessibleOwnerIds(ownerId);
        List<KnowledgeVault> loadedVaults = vaults.findAllByOwnerIdInAndArchivedFalseOrderByUpdatedAtDesc(
                authorizedOwnerIds, Limit.of(MAX_VAULTS + 1));
        boolean truncated = loadedVaults.size() > MAX_VAULTS;
        List<KnowledgeVault> graphVaults = loadedVaults.stream().limit(MAX_VAULTS).toList();

        if (graphVaults.isEmpty()) {
            return new KnowledgeGraphResponse(List.of(), List.of(), 0, 0, 0, false);
        }

        List<UUID> vaultIds = graphVaults.stream().map(KnowledgeVault::getId).toList();
        List<KnowledgeSource> loadedSources = sources.findAuthorizedForGraph(
                authorizedOwnerIds, vaultIds, Limit.of(MAX_SOURCES + 1));
        truncated = truncated || loadedSources.size() > MAX_SOURCES;
        List<KnowledgeSource> graphSources = loadedSources.stream().limit(MAX_SOURCES).toList();

        List<KnowledgeChunk> loadedChunks = graphSources.isEmpty()
                ? List.of()
                : chunks.findAuthorizedForGraph(
                        authorizedOwnerIds,
                        graphSources.stream().map(KnowledgeSource::getId).toList(),
                        Limit.of(MAX_CHUNKS + 1));
        truncated = truncated || loadedChunks.size() > MAX_CHUNKS;
        List<KnowledgeChunk> graphChunks = loadedChunks.stream().limit(MAX_CHUNKS).toList();

        List<KnowledgeGraphNodeResponse> nodes = new ArrayList<>();
        List<KnowledgeGraphEdgeResponse> edges = new ArrayList<>();
        Map<UUID, KnowledgeVault> vaultById = new HashMap<>();
        Map<UUID, KnowledgeSource> sourceById = new HashMap<>();
        Map<UUID, Integer> sourceChunkCounts = new HashMap<>();
        Map<UUID, Team> teamsById = teams.findAllById(graphVaults.stream()
                        .filter(vault -> vault.getOwnerType() == VaultOwnerType.TEAM)
                        .map(KnowledgeVault::getOwnerId)
                        .distinct()
                        .toList()).stream()
                .collect(Collectors.toMap(Team::getId, Function.identity()));

        graphVaults.forEach(vault -> vaultById.put(vault.getId(), vault));
        graphSources.forEach(source -> sourceById.put(source.getId(), source));
        graphChunks.forEach(chunk -> sourceChunkCounts.merge(chunk.getSourceId(), 1, Integer::sum));

        for (KnowledgeVault vault : graphVaults) {
            long sourceCount = graphSources.stream().filter(source -> source.getVaultId().equals(vault.getId())).count();
            nodes.add(new KnowledgeGraphNodeResponse(
                    vaultNodeId(vault.getId()),
                    KnowledgeGraphNodeKind.VAULT,
                    vault.getId(),
                    vault.getOwnerId(),
                    vault.getOwnerType(),
                    ownerName(vault, teamsById),
                    null,
                    null,
                    vault.getName(),
                    sourceCount + (sourceCount == 1 ? " source" : " sources"),
                    vault.getDescription(),
                    vault.getScope().name()));
        }

        for (KnowledgeSource source : graphSources) {
            KnowledgeVault vault = vaultById.get(source.getVaultId());
            if (vault == null) {
                continue;
            }
            int chunkCount = sourceChunkCounts.getOrDefault(source.getId(), 0);
            nodes.add(new KnowledgeGraphNodeResponse(
                    sourceNodeId(source.getId()),
                    KnowledgeGraphNodeKind.SOURCE,
                    vault.getId(),
                    vault.getOwnerId(),
                    vault.getOwnerType(),
                    ownerName(vault, teamsById),
                    source.getId(),
                    null,
                    source.getDisplayName(),
                    chunkCount + (chunkCount == 1 ? " chunk" : " chunks"),
                    null,
                    source.getStatus().name()));
            edges.add(new KnowledgeGraphEdgeResponse(
                    "contains:" + vault.getId() + ":" + source.getId(),
                    KnowledgeGraphRelation.CONTAINS,
                    vaultNodeId(vault.getId()),
                    sourceNodeId(source.getId()),
                    null));
        }

        for (KnowledgeChunk chunk : graphChunks) {
            KnowledgeSource source = sourceById.get(chunk.getSourceId());
            if (source == null) {
                continue;
            }
            KnowledgeVault vault = vaultById.get(source.getVaultId());
            if (vault == null) {
                continue;
            }
            nodes.add(new KnowledgeGraphNodeResponse(
                    chunkNodeId(chunk.getId()),
                    KnowledgeGraphNodeKind.CHUNK,
                    source.getVaultId(),
                    vault.getOwnerId(),
                    vault.getOwnerType(),
                    ownerName(vault, teamsById),
                    source.getId(),
                    chunk.getOrdinal(),
                    "Chunk " + (chunk.getOrdinal() + 1),
                    chunk.getTokenEstimate() + " estimated tokens",
                    excerpt(chunk.getContent()),
                    "EMBEDDED"));
            edges.add(new KnowledgeGraphEdgeResponse(
                    "contains:" + source.getId() + ":" + chunk.getId(),
                    KnowledgeGraphRelation.CONTAINS,
                    sourceNodeId(source.getId()),
                    chunkNodeId(chunk.getId()),
                    null));
        }

        edges.addAll(semanticEdges(graphChunks));

        return new KnowledgeGraphResponse(
                List.copyOf(nodes),
                List.copyOf(edges),
                graphVaults.size(),
                graphSources.size(),
                graphChunks.size(),
                truncated);
    }

    private String ownerName(KnowledgeVault vault, Map<UUID, Team> teamsById) {
        if (vault.getOwnerType() == VaultOwnerType.USER) {
            return "Personal space";
        }
        Team team = teamsById.get(vault.getOwnerId());
        return team == null ? "Team" : team.getName();
    }

    private List<KnowledgeGraphEdgeResponse> semanticEdges(List<KnowledgeChunk> graphChunks) {
        List<KnowledgeGraphEdgeResponse> candidates = new ArrayList<>();
        for (int leftIndex = 0; leftIndex < graphChunks.size(); leftIndex++) {
            KnowledgeChunk left = graphChunks.get(leftIndex);
            for (int rightIndex = leftIndex + 1; rightIndex < graphChunks.size(); rightIndex++) {
                KnowledgeChunk right = graphChunks.get(rightIndex);
                if (left.getSourceId().equals(right.getSourceId())) {
                    continue;
                }
                double similarity = cosineSimilarity(left.getEmbedding(), right.getEmbedding());
                if (similarity < MIN_SEMANTIC_SIMILARITY) {
                    continue;
                }
                candidates.add(new KnowledgeGraphEdgeResponse(
                        "semantic:" + left.getId() + ":" + right.getId(),
                        KnowledgeGraphRelation.SEMANTIC,
                        chunkNodeId(left.getId()),
                        chunkNodeId(right.getId()),
                        roundSimilarity(similarity)));
            }
        }

        candidates.sort(Comparator.comparing(KnowledgeGraphEdgeResponse::similarity).reversed());
        Map<String, Integer> degreeByNode = new HashMap<>();
        List<KnowledgeGraphEdgeResponse> selected = new ArrayList<>();
        for (KnowledgeGraphEdgeResponse candidate : candidates) {
            if (selected.size() >= MAX_SEMANTIC_EDGES) {
                break;
            }
            int fromDegree = degreeByNode.getOrDefault(candidate.fromId(), 0);
            int toDegree = degreeByNode.getOrDefault(candidate.toId(), 0);
            if (fromDegree >= MAX_SEMANTIC_EDGES_PER_CHUNK || toDegree >= MAX_SEMANTIC_EDGES_PER_CHUNK) {
                continue;
            }
            selected.add(candidate);
            degreeByNode.put(candidate.fromId(), fromDegree + 1);
            degreeByNode.put(candidate.toId(), toDegree + 1);
        }
        return selected;
    }

    private double cosineSimilarity(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || left.length != right.length) {
            return -1;
        }
        double dotProduct = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.length; index++) {
            dotProduct += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return -1;
        }
        return Math.max(-1, Math.min(1, dotProduct / Math.sqrt(leftNorm * rightNorm)));
    }

    private double roundSimilarity(double similarity) {
        return Math.round(similarity * 10_000.0) / 10_000.0;
    }

    private String excerpt(String content) {
        String normalized = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= MAX_EXCERPT_CHARACTERS
                ? normalized
                : normalized.substring(0, MAX_EXCERPT_CHARACTERS).stripTrailing() + "…";
    }

    private String vaultNodeId(UUID vaultId) {
        return "vault:" + vaultId;
    }

    private String sourceNodeId(UUID sourceId) {
        return "source:" + sourceId;
    }

    private String chunkNodeId(UUID chunkId) {
        return "chunk:" + chunkId;
    }
}
