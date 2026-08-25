package com.nexoia.knowledge.ingestion.tool;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditOutcome;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.knowledge.embedding.exception.EmbeddingProviderUnavailableException;
import com.nexoia.knowledge.ingestion.service.SourceIngestionService;
import com.nexoia.knowledge.vault.exception.VaultNotWritableException;
import com.nexoia.provider.dto.KnowledgeWriteToolScope;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

/** Builds one request-scoped {@code save_to_vault} tool that appends knowledge to a writable Vault. */
@Component
public class KnowledgeWriteToolFactory {

    public static final String TOOL_NAME = "save_to_vault";
    public static final int MAX_CALLS = 2;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 20_000;

    private final SourceIngestionService ingestion;
    private final AuditService audit;
    private final Clock clock;

    public KnowledgeWriteToolFactory(SourceIngestionService ingestion, AuditService audit, Clock clock) {
        this.ingestion = ingestion;
        this.audit = audit;
        this.clock = clock;
    }

    public KnowledgeWriteToolSession open(
            KnowledgeWriteToolScope scope,
            ToolExecutionObserver observer,
            BooleanSupplier cancelled) {
        List<ToolExecutionEvidence> evidence = new ArrayList<>();
        AtomicInteger callCount = new AtomicInteger();
        Set<String> seen = new HashSet<>();

        var callback = FunctionToolCallback
                .builder(TOOL_NAME, (SaveToVaultInput input, ToolContext ignored) ->
                        execute(scope, observer, cancelled, evidence, callCount, seen, input))
                .description("Append a titled note to the writable Knowledge Vault attached to this conversation, "
                        + "so it can be retrieved in later questions")
                .inputType(SaveToVaultInput.class)
                .build();
        return new KnowledgeWriteToolSession(callback, evidence);
    }

    private SaveToVaultResult execute(
            KnowledgeWriteToolScope scope,
            ToolExecutionObserver observer,
            BooleanSupplier cancelled,
            List<ToolExecutionEvidence> evidence,
            AtomicInteger callCount,
            Set<String> seen,
            SaveToVaultInput input) {
        String title = input == null || input.title() == null ? "" : input.title().trim();
        String content = input == null || input.content() == null ? "" : input.content().strip();
        UUID executionId = UUID.randomUUID();
        Instant startedAt = clock.instant();
        observer.onStarted(new ToolExecutionStarted(executionId, TOOL_NAME, digest(title + "\n" + content), startedAt));
        audit(scope, AuditAction.TOOL_CALL_STARTED, AuditOutcome.SUCCESS, TOOL_NAME);

        if (cancelled.getAsBoolean()
                || !scope.available()
                || title.isBlank()
                || content.isBlank()
                || title.length() > MAX_TITLE_LENGTH
                || content.length() > MAX_CONTENT_LENGTH
                || callCount.incrementAndGet() > MAX_CALLS
                || !seen.add(digest(title + "\n" + content))) {
            return finish(scope, observer, evidence, startedAt, executionId, ToolExecutionStatus.DENIED,
                    "The save was denied by the request policy.");
        }

        try {
            ingestion.saveAgentNote(scope.userId(), scope.vaultId(), title, content);
            return finish(scope, observer, evidence, startedAt, executionId, ToolExecutionStatus.COMPLETED,
                    "Saved to the Knowledge Vault \"" + scope.vaultName()
                            + "\"; it will be available in later searches.");
        } catch (VaultNotWritableException exception) {
            return finish(scope, observer, evidence, startedAt, executionId, ToolExecutionStatus.DENIED,
                    "This Knowledge Vault is not writable.");
        } catch (EmbeddingProviderUnavailableException exception) {
            return finish(scope, observer, evidence, startedAt, executionId, ToolExecutionStatus.UNAVAILABLE,
                    "Saving to the Vault is temporarily unavailable.");
        } catch (RuntimeException exception) {
            return finish(scope, observer, evidence, startedAt, executionId, ToolExecutionStatus.FAILED,
                    "The knowledge could not be saved.");
        }
    }

    private SaveToVaultResult finish(
            KnowledgeWriteToolScope scope,
            ToolExecutionObserver observer,
            List<ToolExecutionEvidence> evidence,
            Instant startedAt,
            UUID executionId,
            ToolExecutionStatus status,
            String message) {
        Instant completedAt = clock.instant();
        ToolExecutionEvidence completed = new ToolExecutionEvidence(
                executionId, TOOL_NAME, status,
                Math.max(0L, completedAt.toEpochMilli() - startedAt.toEpochMilli()),
                List.of(), completedAt);
        evidence.add(completed);
        observer.onCompleted(completed);
        AuditAction action = switch (status) {
            case COMPLETED -> AuditAction.TOOL_CALL_COMPLETED;
            case DENIED -> AuditAction.TOOL_CALL_DENIED;
            default -> AuditAction.TOOL_CALL_FAILED;
        };
        audit(scope, action,
                status == ToolExecutionStatus.COMPLETED ? AuditOutcome.SUCCESS : AuditOutcome.FAILURE,
                TOOL_NAME + ":" + status.name());
        return new SaveToVaultResult(status, scope.vaultName(), message);
    }

    private void audit(KnowledgeWriteToolScope scope, AuditAction action, AuditOutcome outcome, String detail) {
        audit.record(new RecordAuditCommand(
                action, outcome, scope.userId(), null, AuditTargetType.MESSAGE,
                scope.assistantMessageId(), scope.correlationId(), detail));
    }

    private String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
