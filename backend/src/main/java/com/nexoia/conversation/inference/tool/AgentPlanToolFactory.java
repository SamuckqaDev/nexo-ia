package com.nexoia.conversation.inference.tool;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditOutcome;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.conversation.inference.model.AgentPlanStepStatus;
import com.nexoia.provider.dto.AgentPlanStepUpdate;
import com.nexoia.provider.dto.AgentPlanToolScope;
import com.nexoia.provider.dto.AgentPlanUpdate;
import com.nexoia.provider.dto.AgentPlanUpdateObserver;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

/** Builds the governed request-scoped {@code update_plan} Spring AI tool. */
@Slf4j
@Component
public class AgentPlanToolFactory {

    public static final String TOOL_NAME = "update_plan";
    public static final int MAX_UPDATES = 8;
    public static final int MAX_STEPS = 12;
    private static final int MAX_STEP_LENGTH = 240;
    private static final int MAX_EXPLANATION_LENGTH = 500;

    private final AuditService audit;
    private final Clock clock;
    private final AgentTaskDecomposer decomposer;

    public AgentPlanToolFactory(AuditService audit, Clock clock, AgentTaskDecomposer decomposer) {
        this.audit = audit;
        this.clock = clock;
        this.decomposer = decomposer;
    }

    public AgentPlanToolSession open(
            AgentPlanToolScope scope,
            ToolExecutionObserver toolObserver,
            AgentPlanUpdateObserver planObserver,
            BooleanSupplier cancelled) {
        List<ToolExecutionEvidence> evidence = new ArrayList<>();
        AtomicInteger updateCount = new AtomicInteger();
        AtomicInteger revision = new AtomicInteger(1);
        AtomicBoolean modelPlanUpdated = new AtomicBoolean();
        Set<String> seenPlans = new HashSet<>();

        List<String> initialSteps = decomposer.decompose(scope.objective());
        planObserver.onUpdated(initialPlan(revision.get(), initialSteps, false));

        var callback = FunctionToolCallback
                .builder(TOOL_NAME, (UpdatePlanInput input, ToolContext ignored) -> execute(
                        scope, toolObserver, planObserver, cancelled, evidence,
                        updateCount, revision, modelPlanUpdated, seenPlans, input))
                .description("Create or replace the concise implementation plan visible to the user")
                .inputType(UpdatePlanInput.class)
                .build();
        return new AgentPlanToolSession(callback, evidence, () -> {
            if (modelPlanUpdated.compareAndSet(false, true)) {
                planObserver.onUpdated(initialPlan(revision.incrementAndGet(), initialSteps, true));
            }
        });
    }

    private UpdatePlanResult execute(
            AgentPlanToolScope scope,
            ToolExecutionObserver toolObserver,
            AgentPlanUpdateObserver planObserver,
            BooleanSupplier cancelled,
            List<ToolExecutionEvidence> evidence,
            AtomicInteger updateCount,
            AtomicInteger revision,
            AtomicBoolean modelPlanUpdated,
            Set<String> seenPlans,
            UpdatePlanInput input) {
        String serialized = input == null ? "" : input.toString();
        String argumentsDigest = digest(serialized);
        UUID executionId = UUID.randomUUID();
        Instant startedAt = clock.instant();
        toolObserver.onStarted(new ToolExecutionStarted(
                executionId, TOOL_NAME, argumentsDigest, startedAt));
        audit(scope, AuditAction.TOOL_CALL_STARTED, AuditOutcome.SUCCESS, TOOL_NAME);

        if (cancelled.getAsBoolean()
                || updateCount.incrementAndGet() > MAX_UPDATES
                || !seenPlans.add(argumentsDigest)
                || !valid(input)) {
            finish(scope, toolObserver, evidence, startedAt, executionId, ToolExecutionStatus.DENIED);
            return new UpdatePlanResult(
                    ToolExecutionStatus.DENIED, null,
                    "The plan update was denied because it violated the request limits.");
        }

        int nextRevision = revision.incrementAndGet();
        AgentPlanUpdate update = new AgentPlanUpdate(
                nextRevision,
                normalizeExplanation(input.explanation()),
                input.plan().stream()
                        .map(step -> new AgentPlanStepUpdate(step.step().trim(), step.status()))
                        .toList(),
                clock.instant());
        try {
            planObserver.onUpdated(update);
            modelPlanUpdated.set(true);
            finish(scope, toolObserver, evidence, startedAt, executionId, ToolExecutionStatus.COMPLETED);
            return new UpdatePlanResult(
                    ToolExecutionStatus.COMPLETED, nextRevision,
                    "The visible implementation plan was updated.");
        } catch (RuntimeException exception) {
            log.warn("[NEXO-BACK][AGENT] Plan update failed messageId={} reason={}",
                    scope.assistantMessageId(), exception.getClass().getSimpleName());
            finish(scope, toolObserver, evidence, startedAt, executionId, ToolExecutionStatus.FAILED);
            return new UpdatePlanResult(
                    ToolExecutionStatus.FAILED, null,
                    "The implementation plan could not be persisted.");
        }
    }

    private boolean valid(UpdatePlanInput input) {
        if (input == null || input.plan() == null
                || input.plan().isEmpty() || input.plan().size() > MAX_STEPS) {
            return false;
        }
        if (input.explanation() != null && input.explanation().trim().length() > MAX_EXPLANATION_LENGTH) {
            return false;
        }

        long inProgress = input.plan().stream()
                .filter(step -> step != null && step.status() == AgentPlanStepStatus.IN_PROGRESS)
                .count();
        return inProgress <= 1 && input.plan().stream().allMatch(step ->
                step != null
                        && step.status() != null
                        && step.step() != null
                        && !step.step().trim().isEmpty()
                        && step.step().trim().length() <= MAX_STEP_LENGTH);
    }

    private String normalizeExplanation(String explanation) {
        if (explanation == null || explanation.isBlank()) {
            return null;
        }
        return explanation.trim();
    }

    private AgentPlanUpdate initialPlan(int revision, List<String> steps, boolean completed) {
        return new AgentPlanUpdate(
                revision,
                completed
                        ? "Objective completed through the executable steps prepared for this request."
                        : "Objective divided into small, verifiable steps before execution.",
                IntStream.range(0, steps.size())
                        .mapToObj(index -> new AgentPlanStepUpdate(
                                steps.get(index),
                                completed
                                        ? AgentPlanStepStatus.COMPLETED
                                        : index == 0
                                                ? AgentPlanStepStatus.IN_PROGRESS
                                                : AgentPlanStepStatus.PENDING))
                        .toList(),
                clock.instant());
    }

    private void finish(
            AgentPlanToolScope scope,
            ToolExecutionObserver observer,
            List<ToolExecutionEvidence> evidence,
            Instant startedAt,
            UUID executionId,
            ToolExecutionStatus status) {
        Instant completedAt = clock.instant();
        ToolExecutionEvidence completed = new ToolExecutionEvidence(
                executionId,
                TOOL_NAME,
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
        AuditOutcome outcome = status == ToolExecutionStatus.FAILED
                ? AuditOutcome.FAILURE
                : AuditOutcome.SUCCESS;
        audit(scope, action, outcome, TOOL_NAME + ":" + status.name());
    }

    private void audit(
            AgentPlanToolScope scope,
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
