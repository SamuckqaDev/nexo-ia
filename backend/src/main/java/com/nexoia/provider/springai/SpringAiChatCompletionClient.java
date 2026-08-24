package com.nexoia.provider.springai;

import com.nexoia.conversation.chat.model.ConversationMode;
import com.nexoia.conversation.inference.tool.AgentPlanToolFactory;
import com.nexoia.conversation.inference.tool.AgentPlanToolSession;
import com.nexoia.conversation.inference.tool.CapabilityInspectionInput;
import com.nexoia.conversation.inference.tool.CapabilityInspectionResult;
import com.nexoia.conversation.inference.tool.CapabilityInspectionTool;
import com.nexoia.knowledge.retrieval.tool.KnowledgeSearchToolFactory;
import com.nexoia.knowledge.retrieval.tool.KnowledgeSearchToolSession;
import com.nexoia.mcp.runtime.service.McpToolSession;
import com.nexoia.mcp.runtime.service.McpToolSessionFactory;
import com.nexoia.memory.personal.tool.RememberToolFactory;
import com.nexoia.memory.personal.tool.RememberToolSession;
import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.exception.ProviderStreamException;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.model.TokenSource;
import com.nexoia.provider.service.ChatCompletionClient;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallLimitBehavior;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.toolsearch.index.regex.RegexToolIndex;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Streams Ollama chat completions through Spring AI's {@link ChatClient}, Advisor chain, and
 * {@link OllamaChatModel} instead of a hand-written {@code /api/chat} NDJSON parser.
 *
 * <p>Nexo still owns the boundary: it authorizes the endpoint/model, bounds the message history, maps
 * roles, drives cancellation, and translates any transport failure into a controlled business
 * exception that never leaks the provider's endpoint or error text. Spring AI owns the protocol.
 *
 * <p>Answer and reasoning are kept apart: Ollama exposes reasoning as a {@code thinking} property on
 * each streamed message, which is handed to {@code onThinking} and never appended to the persistable
 * answer.
 */
@Slf4j
@Service
public class SpringAiChatCompletionClient implements ChatCompletionClient {

    private static final String THINKING_KEY = "thinking";
    private static final String CANCELLED_REASON = "cancelled";
    private static final String TOOL_DISCOVERY_SESSION_ID = "nexoToolDiscoverySessionId";
    private static final String TOOL_SEARCH_NAME = "toolSearchTool";
    private static final String CAPABILITY_INSPECTION_NAME = "inspect_capabilities";
    private static final int MAX_TOOL_SEARCH_CALLS = 3;
    private static final int MAX_CAPABILITY_INSPECTION_CALLS = 2;
    private static final String TOOL_DISCOVERY_INSTRUCTIONS = """

            Nexo uses progressive tool discovery for this request. Before saying that a capability,
            MCP tool, Knowledge tool, plan tool, or memory tool is unavailable, call
            `toolSearchTool` with a description of the required capability. Tool search only reveals
            tools authorized for this authenticated request. After discovery, invoke the matching
            tool before claiming that an action succeeded. For questions about which tools are
            available, search broadly and answer from the returned tool names; never invent tools.
            """;

    private final SpringAiModelFactory modelFactory;
    private final SpringAiMessageMapper messageMapper;
    private final KnowledgeSearchToolFactory knowledgeToolFactory;
    private final AgentPlanToolFactory planToolFactory;
    private final RememberToolFactory rememberToolFactory;
    private final McpToolSessionFactory mcpToolSessionFactory;
    private final ObservationRegistry observationRegistry;

    public SpringAiChatCompletionClient(
            SpringAiModelFactory modelFactory,
            SpringAiMessageMapper messageMapper,
            KnowledgeSearchToolFactory knowledgeToolFactory,
            AgentPlanToolFactory planToolFactory,
            RememberToolFactory rememberToolFactory,
            McpToolSessionFactory mcpToolSessionFactory,
            ObservationRegistry observationRegistry) {
        this.modelFactory = modelFactory;
        this.messageMapper = messageMapper;
        this.knowledgeToolFactory = knowledgeToolFactory;
        this.planToolFactory = planToolFactory;
        this.rememberToolFactory = rememberToolFactory;
        this.mcpToolSessionFactory = mcpToolSessionFactory;
        this.observationRegistry = observationRegistry;
    }

    @Override
    public boolean supports(ProviderType providerType) {
        return providerType == ProviderType.OLLAMA;
    }

    @Override
    public ChatCompletionOutcome stream(
            ChatCompletionCommand command,
            Consumer<String> onThinking,
            Consumer<String> onToken,
            BooleanSupplier cancelled) {
        AtomicBoolean answerStarted = new AtomicBoolean(false);
        Consumer<String> trackedToken = delta -> {
            answerStarted.set(true);
            onToken.accept(delta);
        };

        try {
            return streamOnce(command, onThinking, trackedToken, cancelled);
        } catch (ProviderStreamException exception) {
            // Ollama rejects `think: true` for models that never advertise reasoning (for example
            // granite). A normal answer is still valid, so retry once without thinking — but only when
            // nothing was streamed yet, so an answer already shown to the user is never duplicated.
            if (!command.thinkingEnabled() || answerStarted.get() || !thinkingUnsupported(exception)) {
                throw exception;
            }
            log.warn("[NEXO-BACK][PROVIDER] Model does not accept thinking; retrying without it model={}",
                    command.model());
            return streamOnce(withoutThinking(command), onThinking, trackedToken, cancelled);
        }
    }

    private ChatCompletionOutcome streamOnce(
            ChatCompletionCommand command,
            Consumer<String> onThinking,
            Consumer<String> onToken,
            BooleanSupplier cancelled) {
        OllamaChatModel model =
                modelFactory.chatModel(command.endpoint(), command.model(), command.thinkingEnabled());
        List<Message> mapped = messageMapper.toSpringAi(command.messages());
        List<SystemMessage> systemContext = new ArrayList<>(mapped.stream()
                .filter(SystemMessage.class::isInstance)
                .map(SystemMessage.class::cast)
                .toList());
        List<Message> conversation = mapped.stream()
                .filter(message -> !(message instanceof SystemMessage))
                .toList();
        Prompt prompt = new Prompt(conversation);
        ChatClient chatClient = ChatClient.builder(model).build();
        KnowledgeSearchToolSession knowledgeSession = knowledgeToolSession(command, cancelled);
        AgentPlanToolSession planSession = planToolSession(command, cancelled);
        RememberToolSession rememberSession = rememberToolSession(command, cancelled);
        McpToolSession mcpSession = mcpToolSession(command, cancelled);
        if (mcpSession != null) {
            systemContext.add(new SystemMessage(mcpRuntimeStatus(mcpSession)));
        }
        List<ToolCallback> callbacks = new ArrayList<>(Stream.of(
                        planSession == null ? null : planSession.callback(),
                        rememberSession == null ? null : rememberSession.callback(),
                        knowledgeSession == null ? null : knowledgeSession.callback())
                .filter(Objects::nonNull)
                .toList());
        if (mcpSession != null) {
            callbacks.addAll(mcpSession.callbacks());
        }
        if (!callbacks.isEmpty()) {
            callbacks.add(capabilityInspectionCallback(callbacks));
        }

        try {
            StringBuilder content = new StringBuilder();
            Integer inputTokens = null;
            Integer outputTokens = null;
            String finishReason = null;

            ChatClient.ChatClientRequestSpec request = chatClient.prompt(prompt)
                    .advisors(new SpringAiContextAdvisor(systemContext));
            if (!callbacks.isEmpty()) {
                ToolCallingManager toolCallingManager = ToolCallingManager.builder()
                        .observationRegistry(observationRegistry)
                        .maxCallsPerTool(2)
                        .maxCallsPerTool(AgentPlanToolFactory.TOOL_NAME, AgentPlanToolFactory.MAX_UPDATES)
                        .maxCallsPerTool(RememberToolFactory.TOOL_NAME, RememberToolFactory.MAX_CALLS)
                        .maxCallsPerTool(KnowledgeSearchToolFactory.TOOL_NAME, KnowledgeSearchToolFactory.MAX_CALLS)
                        .maxCallsPerTool(TOOL_SEARCH_NAME, MAX_TOOL_SEARCH_CALLS)
                        .maxCallsPerTool(CAPABILITY_INSPECTION_NAME, MAX_CAPABILITY_INSPECTION_CALLS)
                        .maxTotalToolCalls(AgentPlanToolFactory.MAX_UPDATES
                                + RememberToolFactory.MAX_CALLS
                                + KnowledgeSearchToolFactory.MAX_CALLS
                                + McpToolSessionFactory.MAX_CALLS
                                + MAX_TOOL_SEARCH_CALLS
                                + MAX_CAPABILITY_INSPECTION_CALLS)
                        .onLimitExceeded(ToolCallLimitBehavior.THROW)
                        .build();
                ToolSearchToolCallingAdvisor toolAdvisor = ToolSearchToolCallingAdvisor.builder()
                        .toolCallingManager(toolCallingManager)
                        .toolIndex(new RegexToolIndex())
                        .sessionIdKeyName(TOOL_DISCOVERY_SESSION_ID)
                        .maxResults(10)
                        .systemMessageSuffix(TOOL_DISCOVERY_INSTRUCTIONS)
                        .build();
                request = request
                        .advisors(toolAdvisor)
                        .advisors(spec -> spec.param(
                                TOOL_DISCOVERY_SESSION_ID,
                                toolDiscoverySessionId(command)))
                        .tools(callbacks.toArray());
            }

            try (Stream<ChatResponse> responses = request
                    .stream()
                    .chatResponse()
                    .toStream()) {
                Iterator<ChatResponse> iterator = responses.iterator();
                while (iterator.hasNext()) {
                    // Consult cancellation before consuming each delta, matching the streaming contract:
                    // closing the Stream disposes the underlying reactive subscription.
                    if (cancelled.getAsBoolean()) {
                        return new ChatCompletionOutcome(
                                content.toString(), null, null, null, true, CANCELLED_REASON,
                                evidence(planSession, rememberSession, knowledgeSession, mcpSession));
                    }

                    ChatResponse response = iterator.next();
                    Generation generation = response.getResult();
                    if (generation != null) {
                        AssistantMessage message = generation.getOutput();
                        if (message.getMetadata().get(THINKING_KEY) instanceof String reasoning
                                && !reasoning.isEmpty()) {
                            onThinking.accept(reasoning);
                        }
                        String delta = message.getText();
                        if (delta != null && !delta.isEmpty()) {
                            content.append(delta);
                            onToken.accept(delta);
                        }
                        if (generation.getMetadata() != null
                                && generation.getMetadata().getFinishReason() != null) {
                            finishReason = generation.getMetadata().getFinishReason();
                        }
                    }

                    Usage usage = response.getMetadata().getUsage();
                    if (usage != null) {
                        if (usage.getPromptTokens() != null) {
                            inputTokens = usage.getPromptTokens();
                        }
                        if (usage.getCompletionTokens() != null) {
                            outputTokens = usage.getCompletionTokens();
                        }
                    }
                }
            } catch (ProviderStreamException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                log.warn("[NEXO-BACK][PROVIDER] Ollama stream failed model={} reason={}",
                        command.model(), exception.getClass().getSimpleName());
                throw new ProviderStreamException(exception);
            }

            TokenSource tokenSource =
                    inputTokens == null && outputTokens == null ? null : TokenSource.PROVIDER;
            if (planSession != null) {
                planSession.completeFallback();
            }
            return new ChatCompletionOutcome(
                    content.toString(), inputTokens, outputTokens, tokenSource, false, finishReason,
                    evidence(planSession, rememberSession, knowledgeSession, mcpSession));
        } finally {
            if (mcpSession != null) {
                mcpSession.close();
            }
        }
    }

    private KnowledgeSearchToolSession knowledgeToolSession(
            ChatCompletionCommand command,
            BooleanSupplier cancelled) {
        if (command.mode() != ConversationMode.AGENT
                || command.knowledgeToolScope() == null
                || !command.knowledgeToolScope().available()) {
            return null;
        }
        return knowledgeToolFactory.open(
                command.knowledgeToolScope(), command.toolExecutionObserver(), cancelled);
    }

    private AgentPlanToolSession planToolSession(
            ChatCompletionCommand command,
            BooleanSupplier cancelled) {
        if (command.mode() != ConversationMode.AGENT || command.agentPlanToolScope() == null) {
            return null;
        }
        return planToolFactory.open(
                command.agentPlanToolScope(),
                command.toolExecutionObserver(),
                command.agentPlanUpdateObserver(),
                cancelled);
    }

    private RememberToolSession rememberToolSession(
            ChatCompletionCommand command,
            BooleanSupplier cancelled) {
        if (command.mode() != ConversationMode.AGENT || command.memoryToolScope() == null) {
            return null;
        }
        return rememberToolFactory.open(
                command.memoryToolScope(), command.toolExecutionObserver(), cancelled);
    }

    private McpToolSession mcpToolSession(
            ChatCompletionCommand command,
            BooleanSupplier cancelled) {
        if (command.mode() != ConversationMode.AGENT
                || command.mcpToolScope() == null
                || !command.mcpToolScope().available()) {
            return null;
        }
        return mcpToolSessionFactory.open(
                command.mcpToolScope(), command.toolExecutionObserver(), cancelled);
    }

    private List<ToolExecutionEvidence> evidence(
            AgentPlanToolSession planSession,
            RememberToolSession rememberSession,
            KnowledgeSearchToolSession knowledgeSession,
            McpToolSession mcpSession) {
        List<ToolExecutionEvidence> evidence = new ArrayList<>();
        if (planSession != null) {
            evidence.addAll(planSession.evidence());
        }
        if (knowledgeSession != null) {
            evidence.addAll(knowledgeSession.evidence());
        }
        if (rememberSession != null) {
            evidence.addAll(rememberSession.evidence());
        }
        if (mcpSession != null) {
            evidence.addAll(mcpSession.evidence());
        }
        return evidence;
    }

    private String mcpRuntimeStatus(McpToolSession session) {
        List<String> toolNames = session.callbacks().stream()
                .map(callback -> callback.getToolDefinition().name())
                .toList();
        if (toolNames.isEmpty()) {
            return "MCP runtime status: the configured MCP connection could not provide any callable "
                    + "tool for this execution. State that limitation and direct the user to inspect the "
                    + "connection in the MCP Hub; do not claim an external action succeeded.";
        }
        return "MCP runtime status: connected. Callable external tools for this execution: "
                + String.join(", ", toolNames) + ". Use a matching tool before claiming external access "
                + "is unavailable, and trust only its returned evidence.";
    }

    private ChatCompletionCommand withoutThinking(ChatCompletionCommand command) {
        return new ChatCompletionCommand(
                command.providerType(), command.endpoint(), command.model(), command.messages(), false,
                command.mode(), command.knowledgeToolScope(), command.agentPlanToolScope(),
                command.memoryToolScope(), command.mcpToolScope(),
                command.toolExecutionObserver(), command.agentPlanUpdateObserver());
    }

    private boolean thinkingUnsupported(ProviderStreamException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof WebClientResponseException.BadRequest badRequest) {
                String body = badRequest.getResponseBodyAsString().toLowerCase();
                return body.contains("think") || body.contains("thinking");
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String toolDiscoverySessionId(ChatCompletionCommand command) {
        if (command.agentPlanToolScope() != null) {
            return command.agentPlanToolScope().assistantMessageId().toString();
        }
        if (command.memoryToolScope() != null) {
            return command.memoryToolScope().assistantMessageId().toString();
        }
        if (command.knowledgeToolScope() != null) {
            return command.knowledgeToolScope().assistantMessageId().toString();
        }
        if (command.mcpToolScope() != null) {
            return command.mcpToolScope().assistantMessageId().toString();
        }
        return UUID.randomUUID().toString();
    }

    private ToolCallback capabilityInspectionCallback(List<ToolCallback> authorizedCallbacks) {
        List<CapabilityInspectionTool> tools = new ArrayList<>();
        tools.add(new CapabilityInspectionTool(
                CAPABILITY_INSPECTION_NAME,
                "Inspect the exact tools authorized for this request"));
        authorizedCallbacks.stream()
                .map(ToolCallback::getToolDefinition)
                .map(definition -> new CapabilityInspectionTool(
                        definition.name(), definition.description()))
                .forEach(tools::add);
        CapabilityInspectionResult result = new CapabilityInspectionResult(
                tools,
                "Only these tools are callable in this request. Discover a matching tool, invoke it, "
                        + "and rely on its result before claiming success. A missing tool is unavailable.");
        return FunctionToolCallback
                .builder(CAPABILITY_INSPECTION_NAME,
                        (CapabilityInspectionInput ignored, ToolContext context) -> result)
                .description("Inspect and list the exact Nexo tools, MCP connections, Knowledge, plan, "
                        + "and memory capabilities authorized for this request")
                .inputType(CapabilityInspectionInput.class)
                .build();
    }
}
