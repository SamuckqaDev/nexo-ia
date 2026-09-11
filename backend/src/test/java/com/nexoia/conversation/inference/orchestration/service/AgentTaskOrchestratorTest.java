package com.nexoia.conversation.inference.orchestration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexoia.conversation.chat.model.ConversationMode;
import com.nexoia.conversation.inference.model.AgentPlanStepStatus;
import com.nexoia.conversation.inference.tool.AgentTaskDecomposer;
import com.nexoia.provider.dto.AgentPlanStepUpdate;
import com.nexoia.provider.dto.AgentPlanToolScope;
import com.nexoia.provider.dto.AgentPlanUpdate;
import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionMessage;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.dto.ToolExecutionObserver;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.model.TokenSource;
import com.nexoia.provider.service.ChatCompletionClient;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class AgentTaskOrchestratorTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-11T12:00:00Z"), ZoneOffset.UTC);
    private final AgentTaskOrchestrator orchestrator =
            new AgentTaskOrchestrator(new AgentTaskDecomposer(), clock);

    @Test
    void plansExecutesEveryTaskSeriallyAndSynthesizesOnlyAtTheEnd() {
        List<AgentPlanUpdate> updates = new ArrayList<>();
        RecordingClient client = new RecordingClient();
        ChatCompletionCommand command = command(updates);

        ChatCompletionOutcome outcome = orchestrator.execute(
                client, command, ignored -> {}, ignored -> {}, () -> false);

        assertThat(client.phases).containsExactly("PLANNING", "TASK:1", "TASK:2", "SYNTHESIS");
        assertThat(updates.getLast().steps())
                .extracting(AgentPlanStepUpdate::status)
                .containsOnly(AgentPlanStepStatus.COMPLETED);
        assertThat(outcome.content()).isEqualTo("final answer");
        assertThat(outcome.inputTokens()).isEqualTo(10);
        assertThat(outcome.outputTokens()).isEqualTo(6);
    }

    @Test
    void stopsBeforeLaterTasksWhenRequiredToolEvidenceIsMissing() {
        List<AgentPlanUpdate> updates = new ArrayList<>();
        RecordingClient client = new RecordingClient();

        ChatCompletionOutcome outcome = orchestrator.execute(
                client,
                command(updates, "Consulte a base de conhecimento e apresente o resultado"),
                ignored -> {},
                ignored -> {},
                () -> false);

        assertThat(outcome.doneReason()).isEqualTo("required_tool_failed");
        assertThat(client.phases).doesNotContain("SYNTHESIS");
        assertThat(updates.getLast().steps())
                .extracting(AgentPlanStepUpdate::status)
                .contains(AgentPlanStepStatus.IN_PROGRESS, AgentPlanStepStatus.PENDING);
    }

    private ChatCompletionCommand command(List<AgentPlanUpdate> updates) {
        return command(updates, "Explique o resultado");
    }

    private ChatCompletionCommand command(List<AgentPlanUpdate> updates, String objective) {
        return new ChatCompletionCommand(
                ProviderType.OLLAMA,
                "http://127.0.0.1:11434",
                "qwen3:8b",
                List.of(new ChatCompletionMessage("user", objective)),
                false,
                ConversationMode.AGENT,
                null,
                new AgentPlanToolScope(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        objective),
                null,
                null,
                null,
                null,
                ToolExecutionObserver.NOOP,
                updates::add);
    }

    private final class RecordingClient implements ChatCompletionClient {

        private final List<String> phases = new ArrayList<>();

        @Override
        public boolean supports(ProviderType providerType) {
            return true;
        }

        @Override
        public ChatCompletionOutcome stream(
                ChatCompletionCommand command,
                Consumer<String> onThinking,
                Consumer<String> onToken,
                BooleanSupplier cancelled) {
            if (command.agentExecution() == null) {
                phases.add("SYNTHESIS");
                onToken.accept("final answer");
                return outcome("final answer", 4, 3);
            }
            if (command.agentExecution().phase().name().equals("PLANNING")) {
                phases.add("PLANNING");
                command.agentPlanUpdateObserver().onUpdated(new AgentPlanUpdate(
                        2,
                        "small tasks",
                        List.of(
                                new AgentPlanStepUpdate(
                                        "Understand the request", null, AgentPlanStepStatus.IN_PROGRESS),
                                new AgentPlanStepUpdate(
                                        "Produce the requested result", null, AgentPlanStepStatus.PENDING)),
                        clock.instant()));
                return outcome("", 2, 1);
            }
            phases.add("TASK:" + command.agentExecution().taskIndex());
            return outcome("task done", 2, 1);
        }

        private ChatCompletionOutcome outcome(String content, int input, int output) {
            return new ChatCompletionOutcome(
                    content, input, output, TokenSource.PROVIDER, false, "stop");
        }
    }
}
