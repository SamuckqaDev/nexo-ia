package com.nexoia.knowledge.retrieval.tool;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditOutcome;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.knowledge.embedding.exception.EmbeddingProviderUnavailableException;
import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.knowledge.retrieval.dto.RetrievalQuery;
import com.nexoia.knowledge.retrieval.service.RetrievalService;
import com.nexoia.provider.dto.KnowledgeToolScope;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.dto.ToolExecutionObserver;
import com.nexoia.provider.dto.ToolExecutionStarted;
import com.nexoia.provider.dto.ToolExecutionStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

/** Builds one request-scoped, read-only Spring AI knowledge tool. */
@Component
public class KnowledgeSearchToolFactory {

    public static final String TOOL_NAME = "search_knowledge";
    public static final int MAX_CALLS = 3;
    private static final int DEFAULT_LIMIT = 4;
    private static final int MAX_LIMIT = 5;
    private static final int MAX_QUERY_LENGTH = 1_000;
    private static final int MAX_EXCERPT_LENGTH = 1_200;

    private final RetrievalService retrieval;
    private final AuditService audit;
    private final Clock clock;

    public KnowledgeSearchToolFactory(RetrievalService retrieval, AuditService audit, Clock clock) {
        this.retrieval = retrieval;
        this.audit = audit;
        this.clock = clock;
    }

    public KnowledgeSearchToolSession open(
            KnowledgeToolScope scope,
            ToolExecutionObserver observer,
            BooleanSupplier cancelled) {
        List<ToolExecutionEvidence> evidence = new ArrayList<>();
        AtomicInteger callCount = new AtomicInteger();
        Set<String> seenQueries = new HashSet<>();

        var callback = FunctionToolCallback
                .builder(TOOL_NAME, (SearchKnowledgeInput input, org.springframework.ai.chat.model.ToolContext ignored) ->
                        execute(scope, observer, cancelled, evidence, callCount, seenQueries, input))
                .description("Search only the Knowledge Vaults already attached and authorized for this conversation")
                .inputType(SearchKnowledgeInput.class)
                .build();
        return new KnowledgeSearchToolSession(callback, evidence);
    }

    private SearchKnowledgeResult execute(
            KnowledgeToolScope scope,
            ToolExecutionObserver observer,
            BooleanSupplier cancelled,
            List<ToolExecutionEvidence> evidence,
            AtomicInteger callCount,
            Set<String> seenQueries,
            SearchKnowledgeInput input) {
        String query = input == null || input.query() == null ? "" : input.query().trim();
        UUID executionId = UUID.randomUUID();
        Instant startedAt = clock.instant();
        ToolExecutionStarted started = new ToolExecutionStarted(
                executionId, TOOL_NAME, digest(query), startedAt);
        observer.onStarted(started);
        audit(scope, AuditAction.TOOL_CALL_STARTED, AuditOutcome.SUCCESS, TOOL_NAME);

        if (cancelled.getAsBoolean()
                || !scope.available()
                || query.isBlank()
                || query.length() > MAX_QUERY_LENGTH
                || callCount.incrementAndGet() > MAX_CALLS
                || !seenQueries.add(query.toLowerCase(Locale.ROOT))) {
            return finish(scope, observer, evidence, startedAt, executionId,
                    ToolExecutionStatus.DENIED, List.of(),
                    "The knowledge search was denied by the request policy.");
        }

        try {
            int limit = input.limit() == null
                    ? DEFAULT_LIMIT
                    : Math.max(1, Math.min(MAX_LIMIT, input.limit()));
            List<CitationResponse> citations = retrieval
                    .retrieve(scope.userId(), new RetrievalQuery(scope.authorizedVaultIds(), query))
                    .citations().stream()
                    .limit(limit)
                    .map(this::bounded)
                    .toList();
            ToolExecutionStatus status = citations.isEmpty()
                    ? ToolExecutionStatus.NO_RESULTS
                    : ToolExecutionStatus.FOUND;
            String message = citations.isEmpty()
                    ? "No relevant source was found in the selected Knowledge Vaults."
                    : "Relevant sources were found in the selected Knowledge Vaults.";
            return finish(scope, observer, evidence, startedAt, executionId, status, citations, message);
        } catch (EmbeddingProviderUnavailableException exception) {
            return finish(scope, observer, evidence, startedAt, executionId,
                    ToolExecutionStatus.UNAVAILABLE, List.of(),
                    "Knowledge search is temporarily unavailable.");
        }
    }

    private SearchKnowledgeResult finish(
            KnowledgeToolScope scope,
            ToolExecutionObserver observer,
            List<ToolExecutionEvidence> evidence,
            Instant startedAt,
            UUID executionId,
            ToolExecutionStatus status,
            List<CitationResponse> citations,
            String message) {
        Instant completedAt = clock.instant();
        ToolExecutionEvidence completed = new ToolExecutionEvidence(
                executionId,
                TOOL_NAME,
                status,
                Math.max(0L, completedAt.toEpochMilli() - startedAt.toEpochMilli()),
                citations,
                completedAt);
        evidence.add(completed);
        observer.onCompleted(completed);
        AuditAction action = switch (status) {
            case DENIED -> AuditAction.TOOL_CALL_DENIED;
            case UNAVAILABLE -> AuditAction.TOOL_CALL_FAILED;
            default -> AuditAction.TOOL_CALL_COMPLETED;
        };
        audit(scope, action,
                status == ToolExecutionStatus.UNAVAILABLE ? AuditOutcome.FAILURE : AuditOutcome.SUCCESS,
                TOOL_NAME + ":" + status.name());
        return new SearchKnowledgeResult(status, citations, message);
    }

    private CitationResponse bounded(CitationResponse citation) {
        String excerpt = citation.excerpt().length() <= MAX_EXCERPT_LENGTH
                ? citation.excerpt()
                : citation.excerpt().substring(0, MAX_EXCERPT_LENGTH);
        return new CitationResponse(
                citation.vaultName(), citation.sourceDisplayName(), citation.chunkOrdinal(),
                excerpt, citation.score());
    }

    private void audit(
            KnowledgeToolScope scope,
            AuditAction action,
            AuditOutcome outcome,
            String detail) {
        audit.record(new RecordAuditCommand(
                action, outcome, scope.userId(), null, AuditTargetType.MESSAGE,
                scope.assistantMessageId(), scope.correlationId(), detail));
    }

    private String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
