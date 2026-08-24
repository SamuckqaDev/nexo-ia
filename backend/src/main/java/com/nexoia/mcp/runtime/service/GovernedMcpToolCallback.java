package com.nexoia.mcp.runtime.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditOutcome;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.provider.dto.McpToolScope;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.dto.ToolExecutionObserver;
import com.nexoia.provider.dto.ToolExecutionStarted;
import com.nexoia.provider.dto.ToolExecutionStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/** Applies Nexo limits, cancellation, audit, and safe failures around one external MCP tool. */
@Slf4j
final class GovernedMcpToolCallback implements ToolCallback {

    private static final int MAX_OUTPUT_LENGTH = 32_000;

    private final ToolCallback delegate;
    private final McpToolScope scope;
    private final ToolExecutionObserver observer;
    private final BooleanSupplier cancelled;
    private final List<ToolExecutionEvidence> evidence;
    private final AtomicInteger totalCalls;
    private final Set<String> seenCalls;
    private final AuditService audit;
    private final Clock clock;

    GovernedMcpToolCallback(
            ToolCallback delegate,
            McpToolScope scope,
            ToolExecutionObserver observer,
            BooleanSupplier cancelled,
            List<ToolExecutionEvidence> evidence,
            AtomicInteger totalCalls,
            Set<String> seenCalls,
            AuditService audit,
            Clock clock) {
        this.delegate = delegate;
        this.scope = scope;
        this.observer = observer;
        this.cancelled = cancelled;
        this.evidence = evidence;
        this.totalCalls = totalCalls;
        this.seenCalls = seenCalls;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return execute(toolInput, () -> delegate.call(toolInput));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return execute(toolInput, () -> delegate.call(toolInput, toolContext));
    }

    private String execute(String toolInput, Supplier<String> invocation) {
        String toolName = getToolDefinition().name();
        String argumentsDigest = digest(toolInput == null ? "" : toolInput);
        UUID executionId = UUID.randomUUID();
        Instant startedAt = clock.instant();
        observer.onStarted(new ToolExecutionStarted(executionId, toolName, argumentsDigest, startedAt));
        audit(AuditAction.TOOL_CALL_STARTED, AuditOutcome.SUCCESS, toolName);

        String callIdentity = toolName + ':' + argumentsDigest;
        if (cancelled.getAsBoolean()
                || totalCalls.incrementAndGet() > McpToolSessionFactory.MAX_CALLS
                || !seenCalls.add(callIdentity)) {
            finish(executionId, startedAt, toolName, ToolExecutionStatus.DENIED);
            return failure("denied", "The MCP tool call was denied by the request policy.");
        }

        try {
            String result = invocation.get();
            finish(executionId, startedAt, toolName, ToolExecutionStatus.COMPLETED);
            if (result == null) {
                return "";
            }
            return result.length() <= MAX_OUTPUT_LENGTH
                    ? result
                    : result.substring(0, MAX_OUTPUT_LENGTH) + "\n[output truncated by Nexo]";
        } catch (RuntimeException exception) {
            log.warn("[NEXO-BACK][MCP] Tool failed name={} messageId={} reason={}",
                    toolName, scope.assistantMessageId(), exception.getClass().getSimpleName());
            finish(executionId, startedAt, toolName, ToolExecutionStatus.FAILED);
            return failure("failed", "The MCP tool failed safely. Try another approach.");
        }
    }

    private void finish(
            UUID executionId,
            Instant startedAt,
            String toolName,
            ToolExecutionStatus status) {
        Instant completedAt = clock.instant();
        ToolExecutionEvidence completed = new ToolExecutionEvidence(
                executionId,
                toolName,
                status,
                Math.max(0L, completedAt.toEpochMilli() - startedAt.toEpochMilli()),
                List.of(),
                completedAt);
        evidence.add(completed);
        observer.onCompleted(completed);
        AuditAction action = switch (status) {
            case DENIED -> AuditAction.TOOL_CALL_DENIED;
            case FAILED, UNAVAILABLE -> AuditAction.TOOL_CALL_FAILED;
            default -> AuditAction.TOOL_CALL_COMPLETED;
        };
        audit(action, status == ToolExecutionStatus.FAILED
                ? AuditOutcome.FAILURE : AuditOutcome.SUCCESS, toolName + ':' + status.name());
    }

    private void audit(AuditAction action, AuditOutcome outcome, String detail) {
        audit.record(new RecordAuditCommand(
                action,
                outcome,
                scope.userId(),
                null,
                AuditTargetType.MESSAGE,
                scope.assistantMessageId(),
                scope.correlationId(),
                detail));
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

    private String failure(String status, String message) {
        return "{\"status\":\"" + status + "\",\"message\":\"" + message + "\"}";
    }
}
