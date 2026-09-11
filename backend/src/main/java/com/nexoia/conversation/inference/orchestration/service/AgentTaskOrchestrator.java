package com.nexoia.conversation.inference.orchestration.service;

import com.nexoia.conversation.inference.model.AgentPlanStepStatus;
import com.nexoia.conversation.inference.tool.AgentTaskDecomposer;
import com.nexoia.conversation.inference.tool.AgentTaskDraft;
import com.nexoia.provider.dto.AgentExecutionDirective;
import com.nexoia.provider.dto.AgentPlanStepUpdate;
import com.nexoia.provider.dto.AgentPlanUpdate;
import com.nexoia.provider.dto.AgentPlanUpdateObserver;
import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionMessage;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.dto.ToolExecutionStatus;
import com.nexoia.provider.exception.ProviderStreamException;
import com.nexoia.provider.service.ChatCompletionClient;
import com.nexoia.workspace.tool.WorkspaceReadToolFactory;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Runs an Agent objective as a server-owned plan, serialized tasks, and final synthesis. */
@Service
public class AgentTaskOrchestrator {

    private static final int MAX_TASK_RESULT_LENGTH = 4_000;
    private static final int MAX_SYNTHESIS_CONTEXT_LENGTH = 16_000;
    private static final int MAX_TASKS = 8;
    private static final String TASK_FAILURE_REASON = "required_tool_failed";

    private final AgentTaskDecomposer decomposer;
    private final Clock clock;

    @Autowired
    public AgentTaskOrchestrator(AgentTaskDecomposer decomposer, Clock clock) {
        this.decomposer = decomposer;
        this.clock = clock;
    }

    public ChatCompletionOutcome execute(
            ChatCompletionClient client,
            ChatCompletionCommand command,
            Consumer<String> onThinking,
            Consumer<String> onToken,
            BooleanSupplier cancelled) {
        AtomicReference<AgentPlanUpdate> latestPlan = new AtomicReference<>();
        AgentPlanUpdateObserver planObserver = update -> {
            latestPlan.set(update);
            command.agentPlanUpdateObserver().onUpdated(update);
        };
        List<ChatCompletionOutcome> outcomes = new ArrayList<>();

        try {
            outcomes.add(client.stream(
                    command.forPlanning(planObserver), onThinking, ignored -> {}, cancelled));
        } catch (ProviderStreamException ignored) {
            // AgentPlanToolFactory publishes a deterministic plan before the model call. It remains
            // the safe execution plan when both the selected model and its fallback ignore planning.
        }
        if (cancelled.getAsBoolean()) {
            return aggregate(outcomes, "", true, "cancelled");
        }

        PlannedExecution execution = executablePlan(command, latestPlan.get());
        AgentPlanUpdate plan = execution.plan();
        int revision = plan.revision();
        List<AgentPlanStepUpdate> steps = resetStatuses(plan.steps());
        List<String> taskResults = new ArrayList<>();
        boolean awaitingApproval = false;

        for (int index = 0; index < steps.size(); index++) {
            if (cancelled.getAsBoolean()) {
                return aggregate(outcomes, "", true, "cancelled");
            }
            steps = withStatus(steps, index, AgentPlanStepStatus.IN_PROGRESS);
            publish(command, ++revision, "Executing one planned task at a time.", steps);

            AgentPlanStepUpdate step = steps.get(index);
            String requiredTool = execution.requiredTools().get(index);
            AgentExecutionDirective directive = AgentExecutionDirective.task(
                    index + 1, steps.size(), step.step(), step.description(), requiredTool);
            ChatCompletionOutcome taskOutcome;
            try {
                taskOutcome = client.stream(
                        command.forTask(directive, taskMessages(command, directive, taskResults)),
                        onThinking,
                        ignored -> {},
                        cancelled);
            } catch (ProviderStreamException exception) {
                publish(command, ++revision,
                        "Execution stopped because the current task was not confirmed by the runtime.",
                        steps);
                return aggregate(outcomes, failureMessage(step), false, TASK_FAILURE_REASON);
            }
            outcomes.add(taskOutcome);
            if (taskOutcome.cancelled()) {
                return aggregate(outcomes, "", true, "cancelled");
            }
            boolean evidenceConfirmed = confirmed(requiredTool, taskOutcome);
            if (failed(taskOutcome) || !evidenceConfirmed) {
                publish(command, ++revision,
                        "Execution stopped because the current task failed. Later tasks were not run.",
                        steps);
                return aggregate(
                        outcomes,
                        value(taskOutcome.content()).isBlank()
                                ? failureMessage(step)
                                : taskOutcome.content(),
                        false,
                        !evidenceConfirmed || taskOutcome.doneReason() == null
                                || taskOutcome.doneReason().isBlank()
                                ? TASK_FAILURE_REASON
                                : taskOutcome.doneReason());
            }
            taskResults.add(taskResult(index, step, taskOutcome));
            steps = withStatus(steps, index, AgentPlanStepStatus.COMPLETED);
            publish(command, ++revision, "Task confirmed; advancing to the next planned task.", steps);
            if (requiresApproval(requiredTool)) {
                int waitingIndex = index + 1;
                if (waitingIndex < steps.size()) {
                    steps = withStatus(steps, waitingIndex, AgentPlanStepStatus.IN_PROGRESS);
                    publish(command, ++revision,
                            "The server prepared the change preview and is waiting for explicit approval.",
                            steps);
                }
                awaitingApproval = true;
                break;
            }
        }

        ChatCompletionOutcome synthesis = client.stream(
                command.forSynthesis(synthesisMessages(command, taskResults)),
                onThinking,
                onToken,
                cancelled);
        outcomes.add(synthesis);
        return aggregate(
                outcomes,
                synthesis.content(),
                synthesis.cancelled(),
                awaitingApproval ? "awaiting_approval" : synthesis.doneReason());
    }

    private PlannedExecution executablePlan(
            ChatCompletionCommand command, AgentPlanUpdate candidate) {
        List<AgentTaskDraft> governedTasks = decomposer.decompose(
                command.agentPlanToolScope().objective());
        boolean requiresGovernedTool = governedTasks.stream()
                .anyMatch(task -> task.requiredToolPrefix() != null);
        if (candidate != null && !candidate.steps().isEmpty() && !requiresGovernedTool) {
            List<AgentPlanStepUpdate> boundedSteps = candidate.steps().stream()
                    .limit(MAX_TASKS)
                    .toList();
            AgentPlanUpdate boundedPlan = candidate.steps().size() == boundedSteps.size()
                    ? candidate
                    : new AgentPlanUpdate(
                            candidate.revision() + 1,
                            "The server bounded the model plan to eight sequential tasks.",
                            boundedSteps,
                            clock.instant());
            if (boundedPlan != candidate) {
                command.agentPlanUpdateObserver().onUpdated(boundedPlan);
            }
            return new PlannedExecution(
                    boundedPlan,
                    boundedSteps.stream()
                            .map(step -> decomposer.requiredToolPrefix(
                                    step.step() + " " + value(step.description())))
                            .toList());
        }
        List<AgentPlanStepUpdate> steps = governedTasks.stream()
                .map(task -> new AgentPlanStepUpdate(
                        task.title(), task.description(), AgentPlanStepStatus.PENDING))
                .toList();
        AgentPlanUpdate fallback = new AgentPlanUpdate(
                candidate == null ? 1 : candidate.revision() + 1,
                requiresGovernedTool
                        ? "The server normalized the model plan into small tasks with enforceable tool evidence."
                        : "The server divided the objective into small, verifiable tasks.",
                steps,
                clock.instant());
        command.agentPlanUpdateObserver().onUpdated(fallback);
        return new PlannedExecution(
                fallback,
                governedTasks.stream().map(AgentTaskDraft::requiredToolPrefix).toList());
    }

    private List<AgentPlanStepUpdate> resetStatuses(List<AgentPlanStepUpdate> steps) {
        return steps.stream()
                .map(step -> new AgentPlanStepUpdate(
                        step.step(), step.description(), AgentPlanStepStatus.PENDING))
                .toList();
    }

    private List<AgentPlanStepUpdate> withStatus(
            List<AgentPlanStepUpdate> steps, int selected, AgentPlanStepStatus status) {
        List<AgentPlanStepUpdate> updated = new ArrayList<>(steps.size());
        for (int index = 0; index < steps.size(); index++) {
            AgentPlanStepUpdate step = steps.get(index);
            AgentPlanStepStatus next = index == selected ? status : step.status();
            updated.add(new AgentPlanStepUpdate(step.step(), step.description(), next));
        }
        return List.copyOf(updated);
    }

    private void publish(
            ChatCompletionCommand command,
            int revision,
            String explanation,
            List<AgentPlanStepUpdate> steps) {
        command.agentPlanUpdateObserver().onUpdated(new AgentPlanUpdate(
                revision, explanation, steps, clock.instant()));
    }

    private List<ChatCompletionMessage> taskMessages(
            ChatCompletionCommand command,
            AgentExecutionDirective task,
            List<String> previousResults) {
        List<ChatCompletionMessage> messages = new ArrayList<>(command.messages());
        messages.add(new ChatCompletionMessage("system", """
                [NEXO_AGENT_TASK]
                Execute only task %d of %d now.
                Title: %s
                Description: %s
                Do not describe commands for the user to run. Use the authorized server tool when
                this task requires one. Do not continue to another task in this response.
                Previous confirmed task results:
                %s
                [/NEXO_AGENT_TASK]
                """.formatted(
                task.taskIndex(),
                task.taskCount(),
                task.title(),
                value(task.description()),
                bounded(String.join("\n", previousResults), MAX_SYNTHESIS_CONTEXT_LENGTH))));
        return List.copyOf(messages);
    }

    private List<ChatCompletionMessage> synthesisMessages(
            ChatCompletionCommand command, List<String> taskResults) {
        List<ChatCompletionMessage> messages = new ArrayList<>(command.messages());
        messages.add(new ChatCompletionMessage("system", """
                [NEXO_AGENT_SYNTHESIS]
                Every planned task below was executed serially by the Nexo server. Produce the final
                concise answer from these confirmed results. Do not invent actions, tool output, files,
                URLs, or completion claims that are absent from the results.

                %s
                [/NEXO_AGENT_SYNTHESIS]
                """.formatted(bounded(
                String.join("\n\n", taskResults), MAX_SYNTHESIS_CONTEXT_LENGTH))));
        return List.copyOf(messages);
    }

    private String taskResult(int index, AgentPlanStepUpdate step, ChatCompletionOutcome outcome) {
        String evidence = outcome.toolExecutions().stream()
                .map(tool -> tool.toolName() + ":" + tool.status())
                .reduce((left, right) -> left + ", " + right)
                .orElse("model-only task");
        return "Task %d — %s\nEvidence: %s\nResult: %s".formatted(
                index + 1,
                step.step(),
                evidence,
                bounded(value(outcome.content()), MAX_TASK_RESULT_LENGTH));
    }

    private boolean failed(ChatCompletionOutcome outcome) {
        return outcome.doneReason() != null && (outcome.doneReason().endsWith("_failed")
                || outcome.doneReason().endsWith("_unavailable"));
    }

    private boolean confirmed(String requiredTool, ChatCompletionOutcome outcome) {
        if (requiredTool == null || requiredTool.isBlank()) {
            return true;
        }
        return outcome.toolExecutions().stream().anyMatch(execution ->
                matches(requiredTool, execution.toolName())
                        && successful(execution.status()));
    }

    private boolean matches(String requiredTool, String actualTool) {
        if (actualTool == null) {
            return false;
        }
        if (AgentTaskDecomposer.WORKSPACE_READ_ONLY.equals(requiredTool)) {
            return List.of(
                            WorkspaceReadToolFactory.LIST_FILES,
                            WorkspaceReadToolFactory.READ_FILE,
                            WorkspaceReadToolFactory.SEARCH,
                            WorkspaceReadToolFactory.GIT_STATUS,
                            WorkspaceReadToolFactory.GIT_DIFF,
                            WorkspaceReadToolFactory.INSPECT_PROJECT)
                    .contains(actualTool);
        }
        return requiredTool.endsWith("_")
                ? actualTool.startsWith(requiredTool)
                : actualTool.equals(requiredTool);
    }

    private boolean successful(ToolExecutionStatus status) {
        return status == ToolExecutionStatus.COMPLETED
                || status == ToolExecutionStatus.FOUND
                || status == ToolExecutionStatus.NO_RESULTS;
    }

    private boolean requiresApproval(String requiredTool) {
        return WorkspaceReadToolFactory.APPLY_PATCH.equals(requiredTool)
                || WorkspaceReadToolFactory.CREATE_FILE.equals(requiredTool)
                || WorkspaceReadToolFactory.DELETE_FILE.equals(requiredTool);
    }

    private String failureMessage(AgentPlanStepUpdate step) {
        return "A tarefa ‘%s’ não foi concluída porque o modelo não executou a ação obrigatória. "
                .formatted(step.step())
                + "A execução foi interrompida para não declarar um resultado falso.";
    }

    private ChatCompletionOutcome aggregate(
            List<ChatCompletionOutcome> outcomes,
            String content,
            boolean cancelled,
            String doneReason) {
        Integer inputTokens = sum(outcomes, true);
        Integer outputTokens = sum(outcomes, false);
        List<ToolExecutionEvidence> evidence = outcomes.stream()
                .flatMap(outcome -> outcome.toolExecutions().stream())
                .toList();
        return new ChatCompletionOutcome(
                content,
                inputTokens,
                outputTokens,
                outcomes.stream()
                        .map(ChatCompletionOutcome::tokenSource)
                        .filter(source -> source != null)
                        .reduce((first, second) -> second)
                        .orElse(null),
                cancelled,
                doneReason,
                evidence);
    }

    private Integer sum(List<ChatCompletionOutcome> outcomes, boolean input) {
        List<Integer> values = outcomes.stream()
                .map(outcome -> input ? outcome.inputTokens() : outcome.outputTokens())
                .filter(value -> value != null)
                .toList();
        return values.isEmpty() ? null : values.stream().mapToInt(Integer::intValue).sum();
    }

    private String bounded(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit) + "…";
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private record PlannedExecution(AgentPlanUpdate plan, List<String> requiredTools) {}
}
