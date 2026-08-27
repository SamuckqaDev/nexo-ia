package com.nexoia.provider.springai;

import com.nexoia.conversation.chat.model.ConversationMode;
import com.nexoia.conversation.inference.tool.AgentPlanToolFactory;
import com.nexoia.conversation.inference.tool.AgentPlanToolSession;
import com.nexoia.conversation.inference.tool.CapabilityInspectionInput;
import com.nexoia.conversation.inference.tool.CapabilityInspectionResult;
import com.nexoia.conversation.inference.tool.CapabilityInspectionTool;
import com.nexoia.knowledge.ingestion.tool.KnowledgeWriteToolFactory;
import com.nexoia.knowledge.ingestion.tool.KnowledgeWriteToolSession;
import com.nexoia.knowledge.retrieval.tool.KnowledgeSearchToolFactory;
import com.nexoia.knowledge.retrieval.tool.KnowledgeSearchToolSession;
import com.nexoia.mcp.runtime.service.McpToolSession;
import com.nexoia.mcp.runtime.service.McpToolSessionFactory;
import com.nexoia.memory.personal.tool.RememberToolFactory;
import com.nexoia.memory.personal.tool.RememberToolSession;
import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionMessage;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.dto.ToolExecutionObserver;
import com.nexoia.provider.dto.ToolExecutionStarted;
import com.nexoia.provider.dto.ToolExecutionStatus;
import com.nexoia.provider.exception.ProviderStreamException;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.model.TokenSource;
import com.nexoia.provider.service.ChatCompletionClient;
import com.nexoia.workspace.tool.WorkspaceReadToolFactory;
import com.nexoia.workspace.tool.WorkspaceReadToolSession;
import io.micrometer.observation.ObservationRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.toolsearch.ToolSearchToolCallingAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
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
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final String USER_REQUEST_MARKER =
            "\n[/NEXO_EXPLICIT_CONTEXT]\n\n[USER_REQUEST]\n";

    private static final String THINKING_KEY = "thinking";
    private static final String CANCELLED_REASON = "cancelled";
    private static final String CAPABILITY_LISTING_REASON = "capability_listing";
    private static final String MCP_UNAVAILABLE_REASON = "mcp_unavailable";
    private static final String MCP_FAILED_REASON = "mcp_failed";
    private static final String TOOL_UNAVAILABLE_REASON = "required_tool_unavailable";
    private static final String TOOL_FAILED_REASON = "required_tool_failed";
    private static final String TOOL_DISCOVERY_SESSION_ID = "nexoToolDiscoverySessionId";
    private static final String TOOL_SEARCH_NAME = "toolSearchTool";
    private static final String CAPABILITY_INSPECTION_NAME = "inspect_capabilities";
    private static final int DIRECT_TOOL_SCHEMA_LIMIT = 10;
    private static final int MAX_TOOL_SEARCH_CALLS = 3;
    private static final int MAX_CAPABILITY_INSPECTION_CALLS = 2;
    private static final int WORKSPACE_PREFLIGHT_RESULT_LIMIT = 6_000;
    private static final List<String> PROJECT_MANIFEST_CANDIDATES = List.of(
            "package.json",
            "pom.xml",
            "build.gradle.kts",
            "build.gradle",
            "pyproject.toml",
            "requirements.txt",
            "go.mod",
            "Cargo.toml");
    private static final List<String> PROJECT_OVERVIEW_CANDIDATES = List.of(
            "README.md", "README", "README.txt");
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
    private final KnowledgeWriteToolFactory knowledgeWriteToolFactory;
    private final AgentPlanToolFactory planToolFactory;
    private final RememberToolFactory rememberToolFactory;
    private final McpToolSessionFactory mcpToolSessionFactory;
    private final WorkspaceReadToolFactory workspaceToolFactory;
    private final ObservationRegistry observationRegistry;
    private final KnowledgeAnswerGrounding knowledgeAnswerGrounding = new KnowledgeAnswerGrounding();
    private final ContentPolicyAnswerGrounding contentPolicyAnswerGrounding =
            new ContentPolicyAnswerGrounding();

    @Autowired
    public SpringAiChatCompletionClient(
            SpringAiModelFactory modelFactory,
            SpringAiMessageMapper messageMapper,
            KnowledgeSearchToolFactory knowledgeToolFactory,
            KnowledgeWriteToolFactory knowledgeWriteToolFactory,
            AgentPlanToolFactory planToolFactory,
            RememberToolFactory rememberToolFactory,
            McpToolSessionFactory mcpToolSessionFactory,
            WorkspaceReadToolFactory workspaceToolFactory,
            ObservationRegistry observationRegistry) {
        this.modelFactory = modelFactory;
        this.messageMapper = messageMapper;
        this.knowledgeToolFactory = knowledgeToolFactory;
        this.knowledgeWriteToolFactory = knowledgeWriteToolFactory;
        this.planToolFactory = planToolFactory;
        this.rememberToolFactory = rememberToolFactory;
        this.mcpToolSessionFactory = mcpToolSessionFactory;
        this.workspaceToolFactory = workspaceToolFactory;
        this.observationRegistry = observationRegistry;
    }

    /** Test/compatibility constructor for requests created before workspace tools were attached. */
    public SpringAiChatCompletionClient(
            SpringAiModelFactory modelFactory,
            SpringAiMessageMapper messageMapper,
            KnowledgeSearchToolFactory knowledgeToolFactory,
            KnowledgeWriteToolFactory knowledgeWriteToolFactory,
            AgentPlanToolFactory planToolFactory,
            RememberToolFactory rememberToolFactory,
            McpToolSessionFactory mcpToolSessionFactory,
            ObservationRegistry observationRegistry) {
        this(modelFactory, messageMapper, knowledgeToolFactory, knowledgeWriteToolFactory,
                planToolFactory, rememberToolFactory, mcpToolSessionFactory, null, observationRegistry);
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
            ChatCompletionOutcome outcome = guardedStreamOnce(
                    command, onThinking, trackedToken, cancelled);
            if (hasAnswer(outcome) || outcome.cancelled()) {
                return outcome;
            }
            if (!outcome.toolExecutions().isEmpty()) {
                return withMissingAnswerFallback(outcome, trackedToken);
            }
            if (command.thinkingEnabled()) {
                log.warn("[NEXO-BACK][PROVIDER] Thinking produced no final answer; retrying without it model={}",
                        command.model());
                ChatCompletionOutcome retried = guardedStreamOnce(
                        withoutThinking(command), onThinking, trackedToken, cancelled);
                ChatCompletionOutcome combined = combineUsage(outcome, retried);
                if (hasAnswer(combined) || combined.cancelled()) {
                    return combined;
                }
                if (!combined.toolExecutions().isEmpty()) {
                    return withMissingAnswerFallback(combined, trackedToken);
                }
            }
            throw new ProviderStreamException(
                    new IllegalStateException("Provider completed without answer content"));
        } catch (ProviderStreamException exception) {
            // Ollama rejects `think: true` for models that never advertise reasoning (for example
            // granite). A normal answer is still valid, so retry once without thinking — but only when
            // nothing was streamed yet, so an answer already shown to the user is never duplicated.
            if (!command.thinkingEnabled() || answerStarted.get() || !thinkingUnsupported(exception)) {
                throw exception;
            }
            log.warn("[NEXO-BACK][PROVIDER] Model does not accept thinking; retrying without it model={}",
                    command.model());
            return guardedStreamOnce(
                    withoutThinking(command), onThinking, trackedToken, cancelled);
        }
    }

    private ChatCompletionOutcome guardedStreamOnce(
            ChatCompletionCommand command,
            Consumer<String> onThinking,
            Consumer<String> onToken,
            BooleanSupplier cancelled) {
        List<ToolEvidenceRequirement> requirements = requiredToolEvidence(command);
        if (requirements.isEmpty()) {
            if (!contentPolicyAnswerGrounding.shouldBuffer(command)) {
                return streamOnce(command, onThinking, onToken, cancelled);
            }
            StringBuilder bufferedAnswer = new StringBuilder();
            ChatCompletionOutcome outcome = streamOnce(
                    command, onThinking, bufferedAnswer::append, cancelled);
            ChatCompletionOutcome grounded = contentPolicyAnswerGrounding.enforce(command, outcome);
            if (hasAnswer(grounded)) {
                onToken.accept(grounded.content());
            }
            return grounded;
        }

        StringBuilder bufferedAnswer = new StringBuilder();
        ChatCompletionOutcome outcome = streamOnce(
                command, onThinking, bufferedAnswer::append, cancelled);
        if (outcome.cancelled()) {
            return outcome;
        }
        if (MCP_UNAVAILABLE_REASON.equals(outcome.doneReason())
                || TOOL_UNAVAILABLE_REASON.equals(outcome.doneReason())) {
            if (hasAnswer(outcome)) {
                onToken.accept(outcome.content());
            }
            return outcome;
        }
        List<ToolEvidenceRequirement> ignored = requirements.stream()
                .filter(requirement -> outcome.toolExecutions().stream()
                        .noneMatch(execution -> requirement.matches(execution.toolName())))
                .toList();
        if (!ignored.isEmpty()) {
            log.warn("[NEXO-BACK][AGENT] Required tools were ignored model={} tools={}",
                    command.model(), ignored.stream().map(ToolEvidenceRequirement::label).toList());
            throw new ProviderStreamException(
                    new IllegalStateException("The model ignored a required governed tool call"));
        }
        List<ToolEvidenceRequirement> failed = requirements.stream()
                .filter(requirement -> outcome.toolExecutions().stream()
                        .filter(execution -> requirement.matches(execution.toolName()))
                        .noneMatch(execution -> successfulEvidence(execution.status())))
                .toList();
        if (!failed.isEmpty()) {
            boolean mcpOnly = failed.stream().allMatch(requirement -> "mcp_".equals(requirement.toolPrefix()));
            String failure = "A ação obrigatória não foi concluída: "
                    + failed.stream().map(ToolEvidenceRequirement::label).collect(Collectors.joining(", "))
                    + ". Consulte o painel Activity para ver o status real.";
            onToken.accept(failure);
            return new ChatCompletionOutcome(
                    failure,
                    outcome.inputTokens(),
                    outcome.outputTokens(),
                    outcome.tokenSource(),
                    false,
                    mcpOnly ? MCP_FAILED_REASON : TOOL_FAILED_REASON,
                    outcome.toolExecutions());
        }
        ChatCompletionOutcome grounded = requiresOnlyKnowledgeEvidence(requirements)
                ? knowledgeAnswerGrounding.enforce(outcome)
                : outcome;
        grounded = contentPolicyAnswerGrounding.enforce(command, grounded);
        if (!Objects.equals(grounded.content(), outcome.content())) {
            log.warn("[NEXO-BACK][ANSWER] Replaced an answer that contradicted deterministic runtime evidence model={}",
                    command.model());
        }
        if (hasAnswer(grounded)) {
            onToken.accept(grounded.content());
        }
        return grounded;
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
        ChatClient chatClient = ChatClient.builder(model).build();
        KnowledgeSearchToolSession knowledgeSession = knowledgeToolSession(command, cancelled);
        KnowledgeWriteToolSession writeSession = knowledgeWriteToolSession(command, cancelled);
        AgentPlanToolSession planSession = planToolSession(command, cancelled);
        RememberToolSession rememberSession = rememberToolSession(command, cancelled);
        McpToolSession mcpSession = mcpToolSession(command, cancelled);
        WorkspaceReadToolSession workspaceSession = workspaceToolSession(command, cancelled);
        List<ToolExecutionEvidence> inspectionEvidence = new ArrayList<>();
        if (mcpSession != null) {
            systemContext.add(new SystemMessage(mcpRuntimeStatus(mcpSession)));
        }
        List<ToolCallback> callbacks = new ArrayList<>(Stream.of(
                        planSession == null ? null : planSession.callback(),
                        rememberSession == null ? null : rememberSession.callback(),
                        knowledgeSession == null ? null : knowledgeSession.callback(),
                        writeSession == null ? null : writeSession.callback())
                .filter(Objects::nonNull)
                .toList());
        if (mcpSession != null) {
            callbacks.addAll(mcpSession.callbacks());
        }
        if (workspaceSession != null) {
            callbacks.addAll(workspaceSession.callbacks());
        }
        List<ToolEvidenceRequirement> requirements = requiredToolEvidence(command);
        List<ToolEvidenceRequirement> missingRequirements = List.of();
        if (!requirements.isEmpty()) {
            List<ToolCallback> requiredCallbacks = requiredCallbacks(command, callbacks, requirements);
            missingRequirements = requirements.stream()
                    .filter(requirement -> requiredCallbacks.stream().noneMatch(callback ->
                            requirement.matches(callback.getToolDefinition().name())))
                    .toList();
            conversation = compactExternalToolConversation(conversation);
            if (requiresProjectAnalysisEvidence(command) && missingRequirements.isEmpty()) {
                String preflight = executeWorkspaceAnalysisPreflight(requiredCallbacks, callbacks);
                callbacks = new ArrayList<>();
                systemContext.add(new SystemMessage(preflight));
            } else {
                callbacks = new ArrayList<>(requiredCallbacks);
                systemContext.add(new SystemMessage(requiredToolInstruction(requirements, callbacks)));
            }
        } else if (!callbacks.isEmpty()) {
            callbacks.add(capabilityInspectionCallback(
                    callbacks, command.toolExecutionObserver(), inspectionEvidence));
        }

        try {
            if (asksForAvailableTools(command)) {
                String listing = capabilityListing(callbacks, command);
                if (planSession != null) {
                    planSession.completeFallback();
                }
                onToken.accept(listing);
                return new ChatCompletionOutcome(
                        listing, null, null, null, false, CAPABILITY_LISTING_REASON,
                        evidence(planSession, rememberSession, knowledgeSession, writeSession,
                                mcpSession, workspaceSession, inspectionEvidence));
            }
            if (!missingRequirements.isEmpty()) {
                boolean mcpOnly = missingRequirements.stream()
                        .allMatch(requirement -> "mcp_".equals(requirement.toolPrefix()));
                String unavailable = mcpOnly
                        ? mcpUnavailableMessage(command)
                        : "Nexo não pode executar esta ação porque falta uma ferramenta autorizada: "
                                + missingRequirements.stream()
                                        .map(ToolEvidenceRequirement::label)
                                        .collect(Collectors.joining(", "))
                                + ". Confira o contexto do Agent antes de tentar novamente.";
                if (planSession != null) {
                    planSession.completeFallback();
                }
                onToken.accept(unavailable);
                return new ChatCompletionOutcome(
                        unavailable, null, null, null, false,
                        mcpOnly ? MCP_UNAVAILABLE_REASON : TOOL_UNAVAILABLE_REASON,
                        evidence(planSession, rememberSession, knowledgeSession, writeSession,
                                mcpSession, workspaceSession, inspectionEvidence));
            }

            Prompt prompt = new Prompt(conversation);
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
                                + WorkspaceReadToolFactory.MAX_CALLS
                                + McpToolSessionFactory.MAX_CALLS
                                + MAX_TOOL_SEARCH_CALLS
                                + MAX_CAPABILITY_INSPECTION_CALLS)
                        .onLimitExceeded(ToolCallLimitBehavior.THROW)
                        .build();
                if (callbacks.size() <= DIRECT_TOOL_SCHEMA_LIMIT) {
                    ToolCallingAdvisor toolAdvisor = ToolCallingAdvisor.builder()
                            .toolCallingManager(toolCallingManager)
                            .build();
                    request = request.advisors(toolAdvisor).tools(callbacks.toArray());
                } else {
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
                                evidence(planSession, rememberSession, knowledgeSession, writeSession,
                                        mcpSession, workspaceSession, inspectionEvidence));
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
                planSession.completeFallback(evidence(
                        planSession, rememberSession, knowledgeSession, writeSession,
                        mcpSession, workspaceSession, inspectionEvidence));
            }
            return new ChatCompletionOutcome(
                    content.toString(), inputTokens, outputTokens, tokenSource, false, finishReason,
                    evidence(planSession, rememberSession, knowledgeSession, writeSession,
                            mcpSession, workspaceSession, inspectionEvidence));
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

    private KnowledgeWriteToolSession knowledgeWriteToolSession(
            ChatCompletionCommand command,
            BooleanSupplier cancelled) {
        if (command.mode() != ConversationMode.AGENT
                || command.knowledgeWriteToolScope() == null
                || !command.knowledgeWriteToolScope().available()) {
            return null;
        }
        return knowledgeWriteToolFactory.open(
                command.knowledgeWriteToolScope(), command.toolExecutionObserver(), cancelled);
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

    private WorkspaceReadToolSession workspaceToolSession(
            ChatCompletionCommand command,
            BooleanSupplier cancelled) {
        if (command.mode() != ConversationMode.AGENT
                || command.workspaceToolScope() == null
                || !command.workspaceToolScope().available()
                || workspaceToolFactory == null) {
            return null;
        }
        return workspaceToolFactory.open(
                command.workspaceToolScope(), command.toolExecutionObserver(), cancelled);
    }

    private List<ToolExecutionEvidence> evidence(
            AgentPlanToolSession planSession,
            RememberToolSession rememberSession,
            KnowledgeSearchToolSession knowledgeSession,
            KnowledgeWriteToolSession writeSession,
            McpToolSession mcpSession,
            WorkspaceReadToolSession workspaceSession,
            List<ToolExecutionEvidence> inspectionEvidence) {
        List<ToolExecutionEvidence> evidence = new ArrayList<>();
        if (planSession != null) {
            evidence.addAll(planSession.evidence());
        }
        if (knowledgeSession != null) {
            evidence.addAll(knowledgeSession.evidence());
        }
        if (writeSession != null) {
            evidence.addAll(writeSession.evidence());
        }
        if (rememberSession != null) {
            evidence.addAll(rememberSession.evidence());
        }
        if (mcpSession != null) {
            evidence.addAll(mcpSession.evidence());
        }
        if (workspaceSession != null) {
            evidence.addAll(workspaceSession.evidence());
        }
        evidence.addAll(inspectionEvidence);
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
                command.memoryToolScope(), command.mcpToolScope(), command.knowledgeWriteToolScope(),
                command.workspaceToolScope(),
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
        if (command.workspaceToolScope() != null) {
            return command.workspaceToolScope().assistantMessageId().toString();
        }
        return UUID.randomUUID().toString();
    }

    private ToolCallback capabilityInspectionCallback(
            List<ToolCallback> authorizedCallbacks,
            ToolExecutionObserver observer,
            List<ToolExecutionEvidence> evidence) {
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
                        (CapabilityInspectionInput input, ToolContext context) -> {
                            Instant startedAt = Instant.now();
                            UUID executionId = UUID.randomUUID();
                            observer.onStarted(new ToolExecutionStarted(
                                    executionId,
                                    CAPABILITY_INSPECTION_NAME,
                                    digest(input == null ? "" : input.focus()),
                                    startedAt));
                            Instant completedAt = Instant.now();
                            ToolExecutionEvidence completed = new ToolExecutionEvidence(
                                    executionId,
                                    CAPABILITY_INSPECTION_NAME,
                                    ToolExecutionStatus.COMPLETED,
                                    Math.max(0L, completedAt.toEpochMilli() - startedAt.toEpochMilli()),
                                    List.of(),
                                    completedAt);
                            evidence.add(completed);
                            observer.onCompleted(completed);
                            return result;
                        })
                .description("Inspect and list the exact Nexo tools, MCP connections, Knowledge, plan, "
                        + "and memory capabilities authorized for this request")
                .inputType(CapabilityInspectionInput.class)
                .build();
    }

    private String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(Objects.requireNonNullElse(value, "").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private boolean hasAnswer(ChatCompletionOutcome outcome) {
        return outcome.content() != null && !outcome.content().isBlank();
    }

    private ChatCompletionOutcome withMissingAnswerFallback(
            ChatCompletionOutcome outcome,
            Consumer<String> onToken) {
        String fallback = "The authorized tool execution finished, but the selected model did not "
                + "produce a final answer. Review the tool evidence and retry with another Agent-ready model.";
        onToken.accept(fallback);
        return new ChatCompletionOutcome(
                fallback,
                outcome.inputTokens(),
                outcome.outputTokens(),
                outcome.tokenSource(),
                false,
                outcome.doneReason(),
                outcome.toolExecutions());
    }

    private ChatCompletionOutcome combineUsage(
            ChatCompletionOutcome first,
            ChatCompletionOutcome second) {
        return new ChatCompletionOutcome(
                second.content(),
                sum(first.inputTokens(), second.inputTokens()),
                sum(first.outputTokens(), second.outputTokens()),
                second.tokenSource() != null ? second.tokenSource() : first.tokenSource(),
                second.cancelled(),
                second.doneReason(),
                Stream.concat(first.toolExecutions().stream(), second.toolExecutions().stream()).toList());
    }

    private Integer sum(Integer first, Integer second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first + second;
    }

    private boolean asksForAvailableTools(ChatCompletionCommand command) {
        String request = normalizedLatestUserRequest(command);
        boolean mentionsTools = request.contains("ferra")
                || request.contains(" tool")
                || request.startsWith("tool")
                || request.contains("mcp");
        boolean namesWorkspaceContent = request.contains("arquivo")
                || request.contains("file")
                || request.contains("pasta")
                || request.contains("diretorio")
                || request.contains("estrutura")
                || request.contains("conteudo")
                || request.contains("raiz")
                || request.contains("git status")
                || request.contains("diff");
        boolean asksForList = request.contains("quais")
                || request.contains("qual")
                || request.contains("lista")
                || request.contains("liste")
                || request.contains("possui")
                || request.contains("disponiv")
                || request.contains("which")
                || request.contains("what")
                || request.contains("available")
                || request.contains("have");
        return command.mode() == ConversationMode.AGENT
                && mentionsTools
                && asksForList
                && !namesWorkspaceContent;
    }

    private boolean requiresExternalToolEvidence(ChatCompletionCommand command) {
        if (command.mode() != ConversationMode.AGENT
                || asksForAvailableTools(command)) {
            return false;
        }
        String request = normalizedLatestUserRequest(command);
        boolean explicitlyExternal = request.contains("internet")
                || request.contains(" web")
                || request.contains("fetch")
                || request.contains("site")
                || request.contains("url")
                || request.contains("cotacao")
                || request.contains("cambio");
        boolean genericLookup = request.contains("pesquis")
                || request.contains("busc")
                || request.contains("procura")
                || request.contains("search")
                || request.contains("look up")
                || request.contains("lookup")
                || request.contains("acesse")
                || request.contains("acessa");
        return explicitlyExternal || (genericLookup
                && !requiresKnowledgeEvidence(command)
                && !requiresWorkspaceEvidence(command));
    }

    private boolean requiresKnowledgeEvidence(ChatCompletionCommand command) {
        if (command.mode() != ConversationMode.AGENT || asksForAvailableTools(command)) {
            return false;
        }
        String request = normalizedLatestUserRequest(command);
        boolean referencesVault = request.contains("base de conhecimento")
                || request.contains("knowledge base")
                || request.contains("knowledge vault")
                || request.contains("vault")
                || request.contains("vout")
                || request.contains("nossa base");
        boolean asksToInspect = request.contains("o que tem")
                || request.contains("lista")
                || request.contains("liste")
                || request.contains("busc")
                || request.contains("consult")
                || request.contains("pesquis")
                || request.contains("conhece")
                || request.contains("sabe")
                || request.contains("fala")
                || request.contains("diz")
                || request.contains("expl");
        return referencesVault && asksToInspect;
    }

    private boolean requiresMemoryEvidence(ChatCompletionCommand command) {
        if (command.mode() != ConversationMode.AGENT || asksForAvailableTools(command)) {
            return false;
        }
        String request = normalizedLatestUserRequest(command);
        return request.contains("lembre")
                || request.contains("memorize")
                || request.contains("guarde na memoria")
                || request.contains("guarda na memoria")
                || request.contains("salve na memoria")
                || request.contains("registre na memoria")
                || (request.contains("guarde") && request.contains("memoria"));
    }

    private boolean requiresWorkspaceEvidence(ChatCompletionCommand command) {
        if (command.mode() != ConversationMode.AGENT || asksForAvailableTools(command)) {
            return false;
        }
        String request = normalizedLatestUserRequest(command);
        boolean referencesWorkspace = request.contains("workspace")
                || request.contains("projeto")
                || request.contains("repositorio")
                || request.contains("repository")
                || request.contains("arquivo")
                || request.contains("file");
        boolean asksToRead = request.contains("lista")
                || request.contains("liste")
                || request.contains("leia")
                || request.contains("ler")
                || request.contains("procure")
                || request.contains("pesquis")
                || request.contains("busc")
                || request.contains("inspec")
                || request.contains("estrutura")
                || request.contains("git status")
                || request.contains("diff")
                || requiresProjectAnalysisEvidence(command);
        return referencesWorkspace && asksToRead;
    }

    private boolean requiresProjectAnalysisEvidence(ChatCompletionCommand command) {
        if (command.mode() != ConversationMode.AGENT || asksForAvailableTools(command)) {
            return false;
        }
        String request = normalizedLatestUserRequest(command);
        boolean referencesProject = request.contains("workspace")
                || request.contains("projeto")
                || request.contains("repositorio")
                || request.contains("repository")
                || request.contains("codebase");
        boolean asksForAnalysis = request.contains("analis")
                || request.contains("avali")
                || request.contains("diagnost")
                || request.contains("review")
                || request.contains("revise")
                || request.contains("entenda")
                || request.contains("entender");
        return referencesProject && asksForAnalysis;
    }

    private List<ToolEvidenceRequirement> requiredToolEvidence(ChatCompletionCommand command) {
        if (asksForAvailableTools(command)) {
            return List.of();
        }
        List<ToolEvidenceRequirement> requirements = new ArrayList<>();
        if (requiresKnowledgeEvidence(command)) {
            requirements.add(new ToolEvidenceRequirement(
                    KnowledgeSearchToolFactory.TOOL_NAME,
                    "consulta aos Knowledge Vaults selecionados"));
        }
        if (requiresExternalToolEvidence(command)) {
            requirements.add(new ToolEvidenceRequirement("mcp_", "consulta pela conexão MCP"));
        }
        if (requiresMemoryEvidence(command)) {
            requirements.add(new ToolEvidenceRequirement(
                    RememberToolFactory.TOOL_NAME,
                    "gravação na memória pessoal"));
        }
        if (requiresProjectAnalysisEvidence(command)) {
            requirements.add(new ToolEvidenceRequirement(
                    WorkspaceReadToolFactory.LIST_FILES,
                    "mapeamento da estrutura do Workspace"));
            requirements.add(new ToolEvidenceRequirement(
                    WorkspaceReadToolFactory.INSPECT_PROJECT,
                    "inspeção da stack e do repositório"));
            requirements.add(new ToolEvidenceRequirement(
                    WorkspaceReadToolFactory.GIT_STATUS,
                    "leitura do estado Git"));
        } else if (requiresWorkspaceEvidence(command)) {
            requirements.add(new ToolEvidenceRequirement(
                    "workspace_",
                    "leitura do Workspace selecionado"));
        }
        return List.copyOf(requirements);
    }

    private String normalizedLatestUserRequest(ChatCompletionCommand command) {
        List<String> requests = command.messages().reversed().stream()
                .filter(message -> "user".equalsIgnoreCase(message.role()))
                .map(ChatCompletionMessage::content)
                .filter(Objects::nonNull)
                .map(this::explicitUserRequest)
                .map(this::normalizeRequest)
                .filter(request -> !request.isBlank())
                .toList();
        if (requests.isEmpty()) {
            return "";
        }
        String latest = requests.getFirst();
        if (!isContinuationRequest(latest)) {
            return latest;
        }
        return requests.stream()
                .skip(1)
                .filter(request -> !isContinuationRequest(request))
                .findFirst()
                .map(previous -> previous + " " + latest)
                .orElse(latest);
    }

    private String explicitUserRequest(String request) {
        int marker = request.indexOf(USER_REQUEST_MARKER);
        return marker < 0
                ? request
                : request.substring(marker + USER_REQUEST_MARKER.length());
    }

    private String normalizeRequest(String request) {
        return Normalizer.normalize(request, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private boolean isContinuationRequest(String request) {
        String compact = request.replaceAll("[.!?,;:]+$", "").trim();
        return compact.matches(
                "(?:pode\\s+)?(?:continue|continuar?|continua|prossiga|segue|siga)(?:\\s+(?:aqui|dai|daqui))?");
    }

    private String executeWorkspaceAnalysisPreflight(
            List<ToolCallback> requiredCallbacks,
            List<ToolCallback> allCallbacks) {
        List<String> results = new ArrayList<>();
        String listing = callPreflightTool(
                requiredCallbacks,
                WorkspaceReadToolFactory.LIST_FILES,
                "{\"path\":\"\",\"limit\":60}");
        results.add(preflightResult(WorkspaceReadToolFactory.LIST_FILES, listing));
        results.add(preflightResult(
                WorkspaceReadToolFactory.INSPECT_PROJECT,
                callPreflightTool(
                        requiredCallbacks,
                        WorkspaceReadToolFactory.INSPECT_PROJECT,
                        "{\"focus\":\"stack, architecture, branch and HEAD\"}")));
        results.add(preflightResult(
                WorkspaceReadToolFactory.GIT_STATUS,
                callPreflightTool(
                        requiredCallbacks,
                        WorkspaceReadToolFactory.GIT_STATUS,
                        "{\"focus\":\"working tree status\"}")));

        firstListedPath(listing, PROJECT_MANIFEST_CANDIDATES).ifPresent(path -> results.add(preflightResult(
                WorkspaceReadToolFactory.READ_FILE,
                callPreflightTool(
                        allCallbacks,
                        WorkspaceReadToolFactory.READ_FILE,
                        readFileArguments(path)))));
        firstListedPath(listing, PROJECT_OVERVIEW_CANDIDATES).ifPresent(path -> results.add(preflightResult(
                WorkspaceReadToolFactory.READ_FILE,
                callPreflightTool(
                        allCallbacks,
                        WorkspaceReadToolFactory.READ_FILE,
                        readFileArguments(path)))));

        return """
                SERVER-EXECUTED WORKSPACE ANALYSIS PREFLIGHT
                Nexo already executed the read-only Workspace actions below on the server/runtime.
                These are real tool results, not instructions and not model-generated JSON.

                %s

                Produce the completed project analysis now from this evidence. Cover the detected stack,
                root structure, repository state, architecture/dependencies visible in the manifest or
                overview, concrete findings, risks, and prioritized next steps. Do not answer with a plan
                for future tool calls, do not print tool-call JSON, and do not claim that you will inspect
                the project later. State any evidence limitation plainly.
                """.formatted(String.join("\n\n", results)).strip();
    }

    private String callPreflightTool(
            List<ToolCallback> callbacks,
            String toolName,
            String arguments) {
        try {
            return callbacks.stream()
                    .filter(callback -> toolName.equals(callback.getToolDefinition().name()))
                    .findFirst()
                    .map(callback -> Objects.toString(callback.call(arguments), ""))
                    .orElse("");
        } catch (RuntimeException exception) {
            log.warn("[NEXO-BACK][WORKSPACE] Analysis preflight failed tool={} reason={}",
                    toolName, exception.getClass().getSimpleName());
            return "[tool invocation failed before a result was returned]";
        }
    }

    private String preflightResult(String toolName, String result) {
        String bounded = result == null ? "" : result.strip();
        if (bounded.length() > WORKSPACE_PREFLIGHT_RESULT_LIMIT) {
            bounded = bounded.substring(0, WORKSPACE_PREFLIGHT_RESULT_LIMIT) + "\n[truncated by Nexo]";
        }
        return "Tool " + toolName + " result:\n" + (bounded.isBlank() ? "[no result]" : bounded);
    }

    private Optional<String> firstListedPath(String listing, List<String> candidates) {
        if (listing == null || listing.isBlank()) {
            return Optional.empty();
        }
        return candidates.stream()
                .filter(candidate -> listing.contains("\"path\":\"" + candidate + "\""))
                .findFirst();
    }

    private String readFileArguments(String path) {
        return "{\"path\":\"" + path + "\",\"startLine\":1,\"endLine\":160}";
    }

    private List<Message> compactExternalToolConversation(List<Message> conversation) {
        List<Message> userMessages = conversation.stream()
                .filter(UserMessage.class::isInstance)
                .toList();
        int first = Math.max(0, userMessages.size() - 6);
        return List.copyOf(userMessages.subList(first, userMessages.size()));
    }

    private List<ToolCallback> requiredCallbacks(
            ChatCompletionCommand command,
            List<ToolCallback> callbacks,
            List<ToolEvidenceRequirement> requirements) {
        LinkedHashMap<String, ToolCallback> selected = new LinkedHashMap<>();
        for (ToolEvidenceRequirement requirement : requirements) {
            List<ToolCallback> matches = callbacks.stream()
                    .filter(callback -> requirement.matches(callback.getToolDefinition().name()))
                    .toList();
            if ("mcp_".equals(requirement.toolPrefix())) {
                matches = preferredExternalCallbacks(command, matches);
            }
            matches.forEach(callback -> selected.put(callback.getToolDefinition().name(), callback));
        }
        return List.copyOf(selected.values());
    }

    private String mcpUnavailableMessage(ChatCompletionCommand command) {
        if (command.mcpToolScope() == null || !command.mcpToolScope().available()) {
            return "Nenhuma ferramenta MCP autorizada está conectada para esta execução. "
                    + "Abra o MCP Hub, conecte e habilite uma ferramenta para o Agent.";
        }
        return "A conexão MCP está configurada, mas não forneceu uma ferramenta callable "
                + "para esta execução. Abra o MCP Hub e inspecione a conexão.";
    }

    private String requiredToolInstruction(
            List<ToolEvidenceRequirement> requirements,
            List<ToolCallback> callbacks) {
        String names = callbacks.stream()
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.joining(", "));
        String labels = requirements.stream()
                .map(ToolEvidenceRequirement::label)
                .collect(Collectors.joining(", "));
        boolean mcpOnly = requirements.size() == 1
                && "mcp_".equals(requirements.getFirst().toolPrefix());
        if (mcpOnly) {
            return """
                    MANDATORY MCP EXECUTION GATE
                    The current user explicitly requested external research or access. The MCP runtime is
                    connected. Ignore earlier assistant claims that external tools are unavailable.
                    Your next response MUST be a tool call to one fitting tool from this exact list: %s.
                    Do not answer with prose and do not call update_plan first. After the tool returns,
                    answer only from its evidence. Never claim MCP is unavailable for this request.
                    """.formatted(names).strip();
        }
        return """
                MANDATORY NEXO TOOL EXECUTION GATE
                The user's request requires these real actions: %s.
                Ignore earlier assistant claims that an action already happened. Your next response
                MUST call
                every required fitting tool from this exact list: %s. Do not call update_plan first.
                After the tools return, answer only from their evidence. Never claim a search,
                external action, or memory write succeeded unless its tool result confirms it.
                For search_knowledge, source names must match citation.sourceDisplayName exactly.
                State a URL only when that exact URL occurs verbatim in a returned citation excerpt.
                If search_knowledge returns no citations, state that no relevant Vault evidence was found.
                """.formatted(labels, names).strip();
    }

    private boolean requiresOnlyKnowledgeEvidence(List<ToolEvidenceRequirement> requirements) {
        return requirements.size() == 1
                && KnowledgeSearchToolFactory.TOOL_NAME.equals(requirements.getFirst().toolPrefix());
    }

    private boolean successfulEvidence(ToolExecutionStatus status) {
        return status == ToolExecutionStatus.COMPLETED
                || status == ToolExecutionStatus.FOUND
                || status == ToolExecutionStatus.NO_RESULTS;
    }

    private List<ToolCallback> preferredExternalCallbacks(
            ChatCompletionCommand command,
            List<ToolCallback> callbacks) {
        String request = normalizedLatestUserRequest(command);
        boolean explicitUrl = request.contains("http://") || request.contains("https://");
        List<ToolCallback> preferred = callbacks.stream()
                .filter(callback -> {
                    String name = callback.getToolDefinition().name().toLowerCase(Locale.ROOT);
                    if (explicitUrl) {
                        return name.contains("fetch")
                                || name.contains("content")
                                || name.contains("open")
                                || name.contains("url");
                    }
                    return name.contains("search")
                            || name.contains("query")
                            || name.contains("find");
                })
                .toList();
        return preferred.isEmpty() ? callbacks : preferred;
    }

    private String capabilityListing(
            List<ToolCallback> callbacks,
            ChatCompletionCommand command) {
        List<String> names = callbacks.stream()
                .map(callback -> callback.getToolDefinition().name())
                .distinct()
                .toList();
        String request = normalizedLatestUserRequest(command);
        String heading = request.contains("which") || request.contains("what") || request.contains("available")
                ? "Tools actually available for this Agent request:"
                : "Ferramentas realmente disponíveis nesta execução do Agente:";
        if (names.isEmpty()) {
            return heading + "\n\n- Nenhuma ferramenta callable foi conectada.";
        }
        return heading + "\n\n" + names.stream()
                .map(name -> "- `" + name + "`")
                .collect(Collectors.joining("\n"));
    }

    private record ToolEvidenceRequirement(String toolPrefix, String label) {
        private boolean matches(String toolName) {
            return toolName != null && toolName.startsWith(toolPrefix);
        }
    }
}
