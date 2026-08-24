package com.nexoia.memory.personal.tool;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditOutcome;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.memory.personal.dto.PersonalMemoryResponse;
import com.nexoia.memory.personal.service.PersonalMemoryService;
import com.nexoia.provider.dto.MemoryToolScope;
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
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

/** Builds the governed request-scoped {@code remember} Spring AI tool. */
@Slf4j
@Component
public class RememberToolFactory {

    public static final String TOOL_NAME = "remember";
    public static final int MAX_CALLS = 2;
    private static final int MAX_CONTENT_LENGTH = 1000;

    private final PersonalMemoryService memories;
    private final AuditService audit;
    private final Clock clock;

    public RememberToolFactory(PersonalMemoryService memories, AuditService audit, Clock clock) {
        this.memories = memories;
        this.audit = audit;
        this.clock = clock;
    }

    public RememberToolSession open(
            MemoryToolScope scope, ToolExecutionObserver observer, BooleanSupplier cancelled) {
        List<ToolExecutionEvidence> evidence = new ArrayList<>();
        AtomicInteger callCount = new AtomicInteger();
        var callback = FunctionToolCallback
                .builder(TOOL_NAME, (RememberInput input, ToolContext ignored) ->
                        execute(scope, observer, cancelled, evidence, callCount, input))
                .description("Store one user-approved personal memory for future Nexo conversations")
                .inputType(RememberInput.class)
                .build();
        return new RememberToolSession(callback, evidence);
    }

    private RememberResult execute(
            MemoryToolScope scope,
            ToolExecutionObserver observer,
            BooleanSupplier cancelled,
            List<ToolExecutionEvidence> evidence,
            AtomicInteger callCount,
            RememberInput input) {
        String content = input == null || input.content() == null ? "" : input.content().trim();
        UUID executionId = UUID.randomUUID();
        Instant startedAt = clock.instant();
        observer.onStarted(new ToolExecutionStarted(executionId, TOOL_NAME, digest(content), startedAt));
        audit(scope, AuditAction.TOOL_CALL_STARTED, AuditOutcome.SUCCESS, TOOL_NAME);

        if (cancelled.getAsBoolean() || content.isBlank() || content.length() > MAX_CONTENT_LENGTH
                || callCount.incrementAndGet() > MAX_CALLS) {
            finish(scope, observer, evidence, startedAt, executionId, ToolExecutionStatus.DENIED);
            return new RememberResult(
                    ToolExecutionStatus.DENIED, null, "The memory was not stored because it violated the request policy.");
        }

        try {
            PersonalMemoryResponse memory = memories.remember(
                    scope.userId(), content, scope.conversationId(), scope.assistantMessageId());
            finish(scope, observer, evidence, startedAt, executionId, ToolExecutionStatus.COMPLETED);
            return new RememberResult(
                    ToolExecutionStatus.COMPLETED, memory.id(), "The personal memory was stored for future conversations.");
        } catch (RuntimeException exception) {
            log.warn("[NEXO-BACK][MEMORY] Remember failed messageId={} reason={}",
                    scope.assistantMessageId(), exception.getClass().getSimpleName());
            finish(scope, observer, evidence, startedAt, executionId, ToolExecutionStatus.FAILED);
            return new RememberResult(
                    ToolExecutionStatus.FAILED, null, "The personal memory could not be stored.");
        }
    }

    private void finish(
            MemoryToolScope scope,
            ToolExecutionObserver observer,
            List<ToolExecutionEvidence> evidence,
            Instant startedAt,
            UUID executionId,
            ToolExecutionStatus status) {
        Instant completedAt = clock.instant();
        ToolExecutionEvidence completed = new ToolExecutionEvidence(
                executionId, TOOL_NAME, status,
                Math.max(0L, completedAt.toEpochMilli() - startedAt.toEpochMilli()),
                List.of(), completedAt);
        evidence.add(completed);
        observer.onCompleted(completed);
        AuditAction action = switch (status) {
            case DENIED -> AuditAction.TOOL_CALL_DENIED;
            case FAILED, UNAVAILABLE -> AuditAction.TOOL_CALL_FAILED;
            default -> AuditAction.TOOL_CALL_COMPLETED;
        };
        audit(scope, action, status == ToolExecutionStatus.FAILED
                ? AuditOutcome.FAILURE : AuditOutcome.SUCCESS, TOOL_NAME + ":" + status.name());
    }

    private void audit(MemoryToolScope scope, AuditAction action, AuditOutcome outcome, String detail) {
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
