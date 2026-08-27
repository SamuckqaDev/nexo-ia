package com.nexoia.provider.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.conversation.chat.model.ConversationMode;
import com.nexoia.conversation.inference.tool.AgentPlanToolFactory;
import com.nexoia.conversation.inference.tool.AgentTaskDecomposer;
import com.nexoia.knowledge.ingestion.tool.KnowledgeWriteToolFactory;
import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.knowledge.retrieval.dto.RetrievalResult;
import com.nexoia.knowledge.retrieval.service.RetrievalService;
import com.nexoia.knowledge.retrieval.tool.KnowledgeSearchToolFactory;
import com.nexoia.mcp.connection.model.McpConnectionKind;
import com.nexoia.mcp.connection.model.McpTransportType;
import com.nexoia.mcp.runtime.dto.McpRuntimeConnection;
import com.nexoia.mcp.runtime.dto.McpRuntimeTool;
import com.nexoia.mcp.runtime.service.McpToolSession;
import com.nexoia.mcp.runtime.service.McpToolSessionFactory;
import com.nexoia.memory.personal.tool.RememberToolFactory;
import com.nexoia.provider.dto.AgentPlanToolScope;
import com.nexoia.provider.dto.AgentPlanUpdate;
import com.nexoia.provider.dto.ChatCompletionCommand;
import com.nexoia.provider.dto.ChatCompletionMessage;
import com.nexoia.provider.dto.ChatCompletionOutcome;
import com.nexoia.provider.dto.KnowledgeToolScope;
import com.nexoia.provider.dto.McpToolScope;
import com.nexoia.provider.dto.MemoryToolScope;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.dto.ToolExecutionObserver;
import com.nexoia.provider.dto.ToolExecutionStatus;
import com.nexoia.provider.dto.WorkspaceToolScope;
import com.nexoia.provider.exception.ProviderStreamException;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.model.TokenSource;
import com.nexoia.workspace.model.WorkspaceAccessMode;
import com.nexoia.workspace.tool.WorkspaceReadToolFactory;
import com.nexoia.workspace.tool.WorkspaceReadToolSession;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.web.client.RestClient;

/**
 * Exercises the Spring AI Ollama adapter against a deterministic local fake, proving that streaming,
 * thinking separation, token accounting, finish reason, cancellation, and failure translation match
 * the compatibility contract without depending on an installed model.
 */
class SpringAiChatCompletionClientTest {

    private static final String STREAM = """
            {"model":"qwen3:8b","message":{"role":"assistant","content":"Hel"},"done":false}
            {"model":"qwen3:8b","message":{"role":"assistant","content":"lo"},"done":false}
            {"model":"qwen3:8b","message":{"role":"assistant","content":""},"done":true,\
            "done_reason":"stop","prompt_eval_count":20,"eval_count":3}
            """;
    private static final String THINKING_STREAM = """
            {"model":"qwen3:8b","message":{"role":"assistant","content":"",\
            "thinking":"Check"},"done":false}
            {"model":"qwen3:8b","message":{"role":"assistant","content":"",\
            "thinking":"ing"},"done":false}
            {"model":"qwen3:8b","message":{"role":"assistant","content":"Done",\
            "thinking":""},"done":false}
            {"model":"qwen3:8b","message":{"role":"assistant","content":""},"done":true,\
            "done_reason":"stop","prompt_eval_count":20,"eval_count":6}
            """;

    private HttpServer server;
    private SpringAiChatCompletionClient client;
    private final AtomicReference<String> requestBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        client = new SpringAiChatCompletionClient(
                new SpringAiModelFactory(RestClient.builder(), ObservationRegistry.NOOP),
                new SpringAiMessageMapper(),
                mock(KnowledgeSearchToolFactory.class),
                mock(KnowledgeWriteToolFactory.class),
                mock(AgentPlanToolFactory.class),
                mock(RememberToolFactory.class),
                mock(McpToolSessionFactory.class),
                ObservationRegistry.NOOP);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void streamsEveryDeltaAndReportsProviderTokenCounts() {
        serve(STREAM);
        StringBuilder streamed = new StringBuilder();

        ChatCompletionOutcome outcome =
                client.stream(command(false), delta -> { }, streamed::append, () -> false);

        assertThat(streamed.toString()).isEqualTo("Hello");
        assertThat(outcome.content()).isEqualTo("Hello");
        assertThat(outcome.inputTokens()).isEqualTo(20);
        assertThat(outcome.outputTokens()).isEqualTo(3);
        assertThat(outcome.tokenSource()).isEqualTo(TokenSource.PROVIDER);
        assertThat(outcome.cancelled()).isFalse();
        assertThat(outcome.doneReason()).isEqualTo("stop");
        assertThat(requestBody.get()).contains("\"model\":\"qwen3:8b\"");
        assertThat(requestBody.get()).contains("\"think\":false");
    }

    @Test
    void separatesThinkingFromThePersistableAnswerWhenEnabled() {
        serve(THINKING_STREAM);
        StringBuilder thinking = new StringBuilder();
        StringBuilder streamed = new StringBuilder();

        ChatCompletionOutcome outcome =
                client.stream(command(true), thinking::append, streamed::append, () -> false);

        assertThat(thinking.toString()).isEqualTo("Checking");
        assertThat(streamed.toString()).isEqualTo("Done");
        assertThat(outcome.content()).isEqualTo("Done");
        assertThat(outcome.content()).doesNotContain("Checking");
        assertThat(requestBody.get()).contains("\"think\":true");
    }

    @Test
    void stopsReadingAndKeepsThePartialAnswerWhenCancelled() {
        serve(STREAM);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        StringBuilder streamed = new StringBuilder();

        ChatCompletionOutcome outcome = client.stream(command(false), delta -> { }, delta -> {
            streamed.append(delta);
            cancelled.set(true);
        }, cancelled::get);

        assertThat(outcome.cancelled()).isTrue();
        assertThat(outcome.content()).isEqualTo("Hel");
        assertThat(outcome.inputTokens()).isNull();
        assertThat(outcome.tokenSource()).isNull();
    }

    @Test
    void reportsAProviderFailureWithoutLeakingTheEndpoint() {
        server.createContext("/api/chat", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();

        assertThatThrownBy(() -> client.stream(command(false), delta -> { }, delta -> { }, () -> false))
                .isInstanceOf(ProviderStreamException.class)
                .hasMessage("The model provider could not complete this request");
    }

    @Test
    void supportsOnlyTheOllamaProviderType() {
        assertThat(client.supports(ProviderType.OLLAMA)).isTrue();
        assertThat(client.supports(ProviderType.OPENAI)).isFalse();
        assertThat(client.supports(ProviderType.ANTHROPIC)).isFalse();
    }

    @Test
    void executesTheGovernedKnowledgeToolAndRejectsALinkOutsideItsEvidence() {
        RetrievalService retrieval = mock(RetrievalService.class);
        when(retrieval.retrieve(any(), any())).thenReturn(new RetrievalResult(List.of(
                new CitationResponse("Nexo KB", "Principles", 1, "Nexo is truthful.", 0.93))));
        SpringAiChatCompletionClient agentClient = new SpringAiChatCompletionClient(
                new SpringAiModelFactory(RestClient.builder(), ObservationRegistry.NOOP),
                new SpringAiMessageMapper(),
                new KnowledgeSearchToolFactory(retrieval, mock(AuditService.class), Clock.systemUTC()),
                mock(KnowledgeWriteToolFactory.class),
                mock(AgentPlanToolFactory.class),
                mock(RememberToolFactory.class),
                mock(McpToolSessionFactory.class),
                ObservationRegistry.NOOP);
        AtomicInteger requests = new AtomicInteger();
        List<String> requestBodies = new ArrayList<>();
        server.createContext("/api/chat", exchange -> {
            requestBodies.add(new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            int requestNumber = requests.incrementAndGet();
            String body = requestNumber == 1
                    ? """
                      {"model":"qwen3:8b","message":{"role":"assistant","content":"",\
                      "tool_calls":[{"id":"call-1","function":{"name":"search_knowledge",\
                      "arguments":{"query":"Nexo identity","limit":2}}}]},"done":true,\
                      "done_reason":"stop","prompt_eval_count":20,"eval_count":1}
                      """
                    : """
                      {"model":"qwen3:8b","message":{"role":"assistant",\
                      "content":"Centro de Suporte Nexo: https://support.nexo.com/unrelated"},\
                      "done":true,"done_reason":"stop",\
                      "prompt_eval_count":30,"eval_count":4}
                      """;
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/x-ndjson");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
        UUID userId = UUID.randomUUID();
        ChatCompletionCommand agentCommand = new ChatCompletionCommand(
                ProviderType.OLLAMA,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "qwen3:8b",
                List.of(new ChatCompletionMessage(
                        "user", "O que a nossa base de conhecimento diz sobre Nexo?")),
                false,
                ConversationMode.AGENT,
                new KnowledgeToolScope(
                        userId, UUID.randomUUID(), UUID.randomUUID(), List.of(UUID.randomUUID())),
                ToolExecutionObserver.NOOP);

        ChatCompletionOutcome outcome = agentClient.stream(
                agentCommand, delta -> {}, delta -> {}, () -> false);

        assertThat(outcome.content())
                .doesNotContain("support.nexo.com")
                .contains("link que não veio dos Knowledge Vaults")
                .contains("Principles")
                .contains("Nexo is truthful.");
        assertThat(outcome.toolExecutions()).hasSize(1);
        assertThat(outcome.toolExecutions().getFirst().citations()).hasSize(1);
        assertThat(requestBodies).hasSize(2);
        assertThat(requestBodies.getFirst())
                .contains("MANDATORY NEXO TOOL EXECUTION GATE")
                .contains("State a URL only when that exact URL occurs verbatim")
                .contains("\"tools\"")
                .contains("\"name\":\"search_knowledge\"")
                .doesNotContain("\"name\":\"inspect_capabilities\"")
                .doesNotContain("toolSearchTool");
        assertThat(requestBodies.get(1))
                .contains("\"role\":\"tool\"")
                .contains("search_knowledge");
    }

    @Test
    void refusesToClaimAMemoryWriteWhenRememberIsNotCallable() {
        ChatCompletionCommand command = new ChatCompletionCommand(
                ProviderType.OLLAMA,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "qwen3:8b",
                List.of(new ChatCompletionMessage(
                        "user", "Guarde na memória que eu sou engenheiro de software")),
                false,
                ConversationMode.AGENT,
                null,
                null,
                new MemoryToolScope(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()),
                null,
                null,
                ToolExecutionObserver.NOOP,
                update -> { });
        StringBuilder streamed = new StringBuilder();

        ChatCompletionOutcome outcome = client.stream(
                command, delta -> { }, streamed::append, () -> false);

        assertThat(outcome.content())
                .contains("falta uma ferramenta autorizada")
                .contains("memória pessoal");
        assertThat(outcome.doneReason()).isEqualTo("required_tool_unavailable");
        assertThat(streamed.toString()).isEqualTo(outcome.content());
        assertThat(requestBody.get()).isNull();
    }

    @Test
    void executesTheVisiblePlanToolThroughTheSpringAiAdvisorLoop() {
        AtomicReference<AgentPlanUpdate> planUpdate = new AtomicReference<>();
        SpringAiChatCompletionClient agentClient = new SpringAiChatCompletionClient(
                new SpringAiModelFactory(RestClient.builder(), ObservationRegistry.NOOP),
                new SpringAiMessageMapper(),
                mock(KnowledgeSearchToolFactory.class),
                mock(KnowledgeWriteToolFactory.class),
                new AgentPlanToolFactory(
                        mock(AuditService.class), Clock.systemUTC(), new AgentTaskDecomposer()),
                mock(RememberToolFactory.class),
                mock(McpToolSessionFactory.class),
                ObservationRegistry.NOOP);
        AtomicInteger requests = new AtomicInteger();
        List<String> requestBodies = new ArrayList<>();
        server.createContext("/api/chat", exchange -> {
            requestBodies.add(new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            int requestNumber = requests.incrementAndGet();
            String body = requestNumber == 1
                    ? """
                      {"model":"qwen3:8b","message":{"role":"assistant","content":"",\
                      "tool_calls":[{"id":"call-plan","function":{"name":"update_plan",\
                      "arguments":{"explanation":"Work visibly","plan":[\
                      {"step":"Inspect","status":"IN_PROGRESS"},\
                      {"step":"Answer","status":"PENDING"}]}}}]},"done":true,\
                      "done_reason":"stop","prompt_eval_count":20,"eval_count":1}
                      """
                    : """
                      {"model":"qwen3:8b","message":{"role":"assistant",\
                      "content":"Plan created."},"done":true,"done_reason":"stop",\
                      "prompt_eval_count":30,"eval_count":4}
                      """;
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/x-ndjson");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
        ChatCompletionCommand agentCommand = new ChatCompletionCommand(
                ProviderType.OLLAMA,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "qwen3:8b",
                List.of(new ChatCompletionMessage("user", "Plan this task")),
                false,
                ConversationMode.AGENT,
                null,
                new AgentPlanToolScope(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Inspect the tools"),
                null,
                ToolExecutionObserver.NOOP,
                planUpdate::set);

        ChatCompletionOutcome outcome = agentClient.stream(
                agentCommand, delta -> {}, delta -> {}, () -> false);

        assertThat(outcome.content()).isEqualTo("Plan created.");
        assertThat(outcome.toolExecutions()).singleElement()
                .satisfies(execution -> assertThat(execution.status()).isEqualTo(ToolExecutionStatus.COMPLETED));
        assertThat(planUpdate.get()).isNotNull();
        assertThat(planUpdate.get().steps()).hasSize(2);
        assertThat(requestBodies).hasSize(2);
        assertThat(requestBodies.getFirst())
                .contains("\"name\":\"update_plan\"")
                .contains("\"name\":\"inspect_capabilities\"")
                .doesNotContain("toolSearchTool")
                .doesNotContain("userId");
        assertThat(requestBodies.get(1))
                .contains("\"role\":\"tool\"")
                .contains("update_plan")
                .doesNotContain("userId");
    }

    @Test
    void listsOnlyTheToolsAuthorizedForTheCurrentRequestWithoutModelGuesswork() {
        SpringAiChatCompletionClient agentClient = new SpringAiChatCompletionClient(
                new SpringAiModelFactory(RestClient.builder(), ObservationRegistry.NOOP),
                new SpringAiMessageMapper(),
                mock(KnowledgeSearchToolFactory.class),
                mock(KnowledgeWriteToolFactory.class),
                new AgentPlanToolFactory(
                        mock(AuditService.class), Clock.systemUTC(), new AgentTaskDecomposer()),
                mock(RememberToolFactory.class),
                mock(McpToolSessionFactory.class),
                ObservationRegistry.NOOP);
        AtomicInteger requests = new AtomicInteger();
        List<String> requestBodies = new ArrayList<>();
        server.createContext("/api/chat", exchange -> {
            requestBodies.add(new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            int requestNumber = requests.incrementAndGet();
            String body = requestNumber == 1
                    ? """
                      {"model":"qwen3:8b","message":{"role":"assistant","content":"",\
                      "tool_calls":[{"id":"call-search-capabilities","function":{\
                      "name":"inspect_capabilities","arguments":{"focus":"tools",\
                      "maxResults":5}}}]},"done":true,"done_reason":"stop",\
                      "prompt_eval_count":10,"eval_count":1}
                      """
                    : requestNumber == 2 ? """
                      {"model":"qwen3:8b","message":{"role":"assistant","content":"",\
                      "tool_calls":[{"id":"call-inspect","function":{\
                      "name":"inspect_capabilities","arguments":{"focus":"tools"}}}]},\
                      "done":true,"done_reason":"stop","prompt_eval_count":20,"eval_count":1}
                      """
                    : """
                      {"model":"qwen3:8b","message":{"role":"assistant",\
                      "content":"I can inspect capabilities and update the plan."},"done":true,\
                      "done_reason":"stop","prompt_eval_count":30,"eval_count":5}
                      """;
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/x-ndjson");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
        ChatCompletionCommand agentCommand = new ChatCompletionCommand(
                ProviderType.OLLAMA,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "qwen3:8b",
                List.of(new ChatCompletionMessage(
                        "user", "Fala pra mim, quais sao as ferramnteas que vc possui...")),
                false,
                ConversationMode.AGENT,
                null,
                new AgentPlanToolScope(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Inspect the tools"),
                null,
                ToolExecutionObserver.NOOP,
                update -> { });

        ChatCompletionOutcome outcome = agentClient.stream(
                agentCommand, delta -> { }, delta -> { }, () -> false);

        assertThat(outcome.content())
                .contains("Ferramentas realmente disponíveis")
                .contains("`update_plan`")
                .contains("`inspect_capabilities`")
                .doesNotContain("search_knowledge")
                .doesNotContain("`remember`")
                .doesNotContain("mcp_");
        assertThat(outcome.doneReason()).isEqualTo("capability_listing");
        assertThat(requestBodies).isEmpty();
    }

    @Test
    void retriesWithoutThinkingWhenTheModelProducesReasoningButNoFinalAnswer() {
        AtomicInteger requests = new AtomicInteger();
        List<String> requestBodies = new ArrayList<>();
        server.createContext("/api/chat", exchange -> {
            requestBodies.add(new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String body = requests.incrementAndGet() == 1
                    ? """
                      {"model":"qwen3:8b","message":{"role":"assistant","content":"",\
                      "thinking":"I should answer."},"done":false}
                      {"model":"qwen3:8b","message":{"role":"assistant","content":""},\
                      "done":true,"done_reason":"stop","prompt_eval_count":12,"eval_count":5}
                      """
                    : """
                      {"model":"qwen3:8b","message":{"role":"assistant",\
                      "content":"Available tools listed."},"done":true,"done_reason":"stop",\
                      "prompt_eval_count":13,"eval_count":4}
                      """;
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/x-ndjson");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();

        ChatCompletionOutcome outcome = client.stream(
                command(true), delta -> { }, delta -> { }, () -> false);

        assertThat(outcome.content()).isEqualTo("Available tools listed.");
        assertThat(outcome.inputTokens()).isEqualTo(25);
        assertThat(outcome.outputTokens()).isEqualTo(9);
        assertThat(requestBodies).hasSize(2);
        assertThat(requestBodies.getFirst()).contains("\"think\":true");
        assertThat(requestBodies.get(1)).contains("\"think\":false");
    }

    @Test
    void tellsTheModelWhenConfiguredMcpHasNoCallableRuntimeTool() {
        McpToolSessionFactory mcpFactory = mock(McpToolSessionFactory.class);
        McpToolSession mcpSession = mock(McpToolSession.class);
        when(mcpSession.callbacks()).thenReturn(List.of());
        when(mcpSession.evidence()).thenReturn(List.of());
        when(mcpFactory.open(any(), any(), any())).thenReturn(mcpSession);
        SpringAiChatCompletionClient agentClient = new SpringAiChatCompletionClient(
                new SpringAiModelFactory(RestClient.builder(), ObservationRegistry.NOOP),
                new SpringAiMessageMapper(),
                mock(KnowledgeSearchToolFactory.class),
                mock(KnowledgeWriteToolFactory.class),
                mock(AgentPlanToolFactory.class),
                mock(RememberToolFactory.class),
                mcpFactory,
                ObservationRegistry.NOOP);
        serve(STREAM);
        UUID userId = UUID.randomUUID();
        McpRuntimeConnection connection = new McpRuntimeConnection(
                UUID.randomUUID(),
                "Search server",
                McpConnectionKind.CUSTOM_REMOTE,
                McpTransportType.STREAMABLE_HTTP,
                null,
                "https://mcp.example.com/mcp",
                List.of(new McpRuntimeTool("search", "mcp_search")));
        ChatCompletionCommand agentCommand = new ChatCompletionCommand(
                ProviderType.OLLAMA,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "qwen3:8b",
                List.of(new ChatCompletionMessage("user", "Search LinkedIn")),
                false,
                ConversationMode.AGENT,
                null,
                null,
                new McpToolScope(userId, UUID.randomUUID(), UUID.randomUUID(), List.of(connection)),
                ToolExecutionObserver.NOOP,
                update -> { });

        ChatCompletionOutcome outcome = agentClient.stream(
                agentCommand, delta -> { }, delta -> { }, () -> false);

        assertThat(outcome.content())
                .contains("não forneceu uma ferramenta callable")
                .contains("MCP Hub");
        assertThat(outcome.doneReason()).isEqualTo("mcp_unavailable");
        assertThat(requestBody.get()).isNull();
        verify(mcpSession).close();
    }

    @Test
    void reportsWhenExternalResearchHasNoAuthorizedMcpConnection() {
        ChatCompletionCommand command = new ChatCompletionCommand(
                ProviderType.OLLAMA,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "qwen3:8b",
                List.of(new ChatCompletionMessage(
                        "user", "Pesquise na internet a documentação atual do Spring AI")),
                false,
                ConversationMode.AGENT,
                null,
                null,
                null,
                null,
                null,
                ToolExecutionObserver.NOOP,
                update -> { });
        StringBuilder streamed = new StringBuilder();

        ChatCompletionOutcome outcome = client.stream(
                command, delta -> { }, streamed::append, () -> false);

        assertThat(outcome.content())
                .contains("Nenhuma ferramenta MCP autorizada")
                .contains("MCP Hub");
        assertThat(outcome.doneReason()).isEqualTo("mcp_unavailable");
        assertThat(streamed.toString()).isEqualTo(outcome.content());
        assertThat(requestBody.get()).isNull();
    }

    @Test
    void listsTheActualAuthorizedMcpCallbackWithoutAskingTheModel() {
        ToolDefinition definition = mock(ToolDefinition.class);
        when(definition.name()).thenReturn("mcp_private_search");
        when(definition.description()).thenReturn("Search public web pages");
        when(definition.inputSchema()).thenReturn("{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}}}");
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(definition);
        McpToolSessionFactory mcpFactory = mock(McpToolSessionFactory.class);
        McpToolSession mcpSession = mock(McpToolSession.class);
        when(mcpSession.callbacks()).thenReturn(List.of(callback));
        when(mcpSession.evidence()).thenReturn(List.of());
        when(mcpFactory.open(any(), any(), any())).thenReturn(mcpSession);
        SpringAiChatCompletionClient agentClient = new SpringAiChatCompletionClient(
                new SpringAiModelFactory(RestClient.builder(), ObservationRegistry.NOOP),
                new SpringAiMessageMapper(),
                mock(KnowledgeSearchToolFactory.class),
                mock(KnowledgeWriteToolFactory.class),
                mock(AgentPlanToolFactory.class),
                mock(RememberToolFactory.class),
                mcpFactory,
                ObservationRegistry.NOOP);
        AtomicInteger requests = new AtomicInteger();
        List<String> requestBodies = new ArrayList<>();
        server.createContext("/api/chat", exchange -> {
            requestBodies.add(new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String body = requests.incrementAndGet() == 1
                    ? """
                      {"model":"qwen3:8b","message":{"role":"assistant","content":"",\
                      "tool_calls":[{"id":"call-inspect","function":{\
                      "name":"inspect_capabilities","arguments":{"focus":"MCP"}}}]},\
                      "done":true,"done_reason":"stop","prompt_eval_count":20,"eval_count":1}
                      """
                    : """
                      {"model":"qwen3:8b","message":{"role":"assistant",\
                      "content":"I can use mcp_private_search."},"done":true,\
                      "done_reason":"stop","prompt_eval_count":30,"eval_count":5}
                      """;
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/x-ndjson");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
        UUID userId = UUID.randomUUID();
        McpRuntimeConnection connection = new McpRuntimeConnection(
                UUID.randomUUID(),
                "Private search",
                McpConnectionKind.CUSTOM_REMOTE,
                McpTransportType.STREAMABLE_HTTP,
                null,
                "https://mcp.example.com/mcp",
                List.of(new McpRuntimeTool("search", "mcp_private_search")));
        ChatCompletionCommand command = new ChatCompletionCommand(
                ProviderType.OLLAMA,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "qwen3:8b",
                List.of(new ChatCompletionMessage("user", "Which tools can you use?")),
                false,
                ConversationMode.AGENT,
                null,
                null,
                new McpToolScope(userId, UUID.randomUUID(), UUID.randomUUID(), List.of(connection)),
                ToolExecutionObserver.NOOP,
                update -> { });

        ChatCompletionOutcome outcome = agentClient.stream(
                command, delta -> { }, delta -> { }, () -> false);

        assertThat(outcome.content())
                .contains("Tools actually available")
                .contains("`mcp_private_search`")
                .contains("`inspect_capabilities`")
                .doesNotContain("toolSearchTool");
        assertThat(outcome.doneReason()).isEqualTo("capability_listing");
        assertThat(requestBodies).isEmpty();
        verify(mcpSession).close();
    }

    @Test
    void listsTheActualWorkspaceCallbackWithoutAskingTheModel() {
        ToolCallback callback = mockCallback("workspace_search", "Search the attached project");
        WorkspaceReadToolFactory workspaceFactory = mock(WorkspaceReadToolFactory.class);
        when(workspaceFactory.open(any(), any(), any())).thenReturn(new WorkspaceReadToolSession(
                List.of(callback), new ArrayList<>()));
        SpringAiChatCompletionClient agentClient = clientWithWorkspaceFactory(workspaceFactory);
        WorkspaceToolScope scope = workspaceScope();
        ChatCompletionCommand command = workspaceCommand(scope, "Quais ferramentas estão disponíveis?");

        ChatCompletionOutcome outcome = agentClient.stream(
                command, delta -> { }, delta -> { }, () -> false);

        assertThat(outcome.content())
                .contains("Ferramentas realmente disponíveis")
                .contains("`workspace_search`")
                .contains("`inspect_capabilities`");
        assertThat(outcome.doneReason()).isEqualTo("capability_listing");
        assertThat(requestBody.get()).isNull();
        verify(workspaceFactory).open(any(), any(), any());
    }

    @Test
    void treatsProjectSearchAsWorkspaceWorkInsteadOfExternalMcpResearch() {
        WorkspaceReadToolFactory workspaceFactory = mock(WorkspaceReadToolFactory.class);
        ToolCallback callback = mockCallback("workspace_search", "Search the attached project");
        when(workspaceFactory.open(any(), any(), any())).thenReturn(new WorkspaceReadToolSession(
                List.of(callback), new ArrayList<>()));
        SpringAiChatCompletionClient agentClient = clientWithWorkspaceFactory(workspaceFactory);
        serve("""
                {"model":"granite4.1:8b","message":{"role":"assistant",\
                "content":"Achei o arquivo."},"done":true,"done_reason":"stop",\
                "prompt_eval_count":20,"eval_count":8}
                """);

        assertThatThrownBy(() -> agentClient.stream(
                workspaceCommand(workspaceScope(), "Pesquise no projeto pelo arquivo README"),
                delta -> { }, delta -> { }, () -> false))
                .isInstanceOf(ProviderStreamException.class);

        assertThat(requestBody.get())
                .contains("MANDATORY NEXO TOOL EXECUTION GATE")
                .contains("workspace_search")
                .contains("leitura do Workspace selecionado")
                .doesNotContain("consulta pela conexão MCP")
                .doesNotContain("MANDATORY MCP EXECUTION GATE");
    }

    @Test
    void requiresAndVerifiesMcpEvidenceForAnExplicitResearchRequest() {
        List<ToolExecutionEvidence> evidence = new ArrayList<>();
        ToolCallback callback = FunctionToolCallback
                .builder("mcp_private_search", (SearchInput input) -> {
                    evidence.add(new ToolExecutionEvidence(
                            UUID.randomUUID(),
                            "mcp_private_search",
                            ToolExecutionStatus.COMPLETED,
                            4L,
                            List.of(),
                            Instant.now()));
                    return "USD/BRL 5.45 from the connected search service";
                })
                .description("Search public web pages")
                .inputType(SearchInput.class)
                .build();
        McpToolSessionFactory mcpFactory = mock(McpToolSessionFactory.class);
        McpToolSession mcpSession = mock(McpToolSession.class);
        when(mcpSession.callbacks()).thenReturn(List.of(callback));
        when(mcpSession.evidence()).thenAnswer(ignored -> List.copyOf(evidence));
        when(mcpFactory.open(any(), any(), any())).thenReturn(mcpSession);
        SpringAiChatCompletionClient agentClient = new SpringAiChatCompletionClient(
                new SpringAiModelFactory(RestClient.builder(), ObservationRegistry.NOOP),
                new SpringAiMessageMapper(),
                mock(KnowledgeSearchToolFactory.class),
                mock(KnowledgeWriteToolFactory.class),
                mock(AgentPlanToolFactory.class),
                mock(RememberToolFactory.class),
                mcpFactory,
                ObservationRegistry.NOOP);
        AtomicInteger requests = new AtomicInteger();
        List<String> requestBodies = new ArrayList<>();
        server.createContext("/api/chat", exchange -> {
            requestBodies.add(new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String body = requests.incrementAndGet() == 1
                    ? """
                      {"model":"granite4.1:8b","message":{"role":"assistant","content":"",\
                      "tool_calls":[{"id":"call-search","function":{\
                      "name":"mcp_private_search","arguments":{"query":"cotacao dolar real"}}}]},\
                      "done":true,"done_reason":"stop","prompt_eval_count":20,"eval_count":4}
                      """
                    : """
                      {"model":"granite4.1:8b","message":{"role":"assistant",\
                      "content":"A busca retornou USD/BRL 5,45."},"done":true,\
                      "done_reason":"stop","prompt_eval_count":35,"eval_count":8}
                      """;
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/x-ndjson");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
        StringBuilder streamed = new StringBuilder();

        ChatCompletionOutcome outcome = agentClient.stream(
                externalResearchCommand(), delta -> { }, streamed::append, () -> false);

        assertThat(outcome.content()).isEqualTo("A busca retornou USD/BRL 5,45.");
        assertThat(streamed.toString()).isEqualTo(outcome.content());
        assertThat(outcome.toolExecutions()).singleElement()
                .satisfies(execution -> assertThat(execution.toolName()).isEqualTo("mcp_private_search"));
        assertThat(requestBodies).hasSize(2);
        assertThat(requestBodies.getFirst())
                .contains("MANDATORY MCP EXECUTION GATE")
                .contains("mcp_private_search")
                .contains("Cara pesquisa direito ai cara")
                .doesNotContain("Infelizmente não tenho acesso");
        assertThat(requestBodies.get(1)).contains("USD/BRL 5.45");
        verify(mcpSession).close();
    }

    @Test
    void directsResearchToSearchBeforeFetchWhenBothToolsAreAuthorized() {
        ToolCallback search = mockCallback("mcp_private_search", "Search the public web");
        ToolCallback fetch = mockCallback("mcp_fetch", "Fetch one known URL");
        McpToolSessionFactory mcpFactory = mock(McpToolSessionFactory.class);
        McpToolSession mcpSession = mock(McpToolSession.class);
        when(mcpSession.callbacks()).thenReturn(List.of(fetch, search));
        when(mcpSession.evidence()).thenReturn(List.of());
        when(mcpFactory.open(any(), any(), any())).thenReturn(mcpSession);
        SpringAiChatCompletionClient agentClient = new SpringAiChatCompletionClient(
                new SpringAiModelFactory(RestClient.builder(), ObservationRegistry.NOOP),
                new SpringAiMessageMapper(),
                mock(KnowledgeSearchToolFactory.class),
                mock(KnowledgeWriteToolFactory.class),
                mock(AgentPlanToolFactory.class),
                mock(RememberToolFactory.class),
                mcpFactory,
                ObservationRegistry.NOOP);
        serve("""
                {"model":"granite4.1:8b","message":{"role":"assistant",\
                "content":"Usei a busca."},"done":true,"done_reason":"stop",\
                "prompt_eval_count":20,"eval_count":8}
                """);

        assertThatThrownBy(() -> agentClient.stream(
                externalResearchCommand(), delta -> { }, delta -> { }, () -> false))
                .isInstanceOf(ProviderStreamException.class);

        String body = requestBody.get();
        String gate = body.substring(body.indexOf("MANDATORY MCP EXECUTION GATE"));
        assertThat(gate).contains("mcp_private_search");
        assertThat(gate.substring(0, gate.indexOf("Your next response")))
                .doesNotContain("mcp_fetch");
    }

    @Test
    void discardsAModelsFalseUnavailableAnswerWhenResearchRequiredMcp() {
        ToolDefinition definition = mock(ToolDefinition.class);
        when(definition.name()).thenReturn("mcp_private_search");
        when(definition.description()).thenReturn("Search public web pages");
        when(definition.inputSchema()).thenReturn("{\"type\":\"object\"}");
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(definition);
        McpToolSessionFactory mcpFactory = mock(McpToolSessionFactory.class);
        McpToolSession mcpSession = mock(McpToolSession.class);
        when(mcpSession.callbacks()).thenReturn(List.of(callback));
        when(mcpSession.evidence()).thenReturn(List.of());
        when(mcpFactory.open(any(), any(), any())).thenReturn(mcpSession);
        SpringAiChatCompletionClient agentClient = new SpringAiChatCompletionClient(
                new SpringAiModelFactory(RestClient.builder(), ObservationRegistry.NOOP),
                new SpringAiMessageMapper(),
                mock(KnowledgeSearchToolFactory.class),
                mock(KnowledgeWriteToolFactory.class),
                mock(AgentPlanToolFactory.class),
                mock(RememberToolFactory.class),
                mcpFactory,
                ObservationRegistry.NOOP);
        serve("""
                {"model":"granite4.1:8b","message":{"role":"assistant",\
                "content":"Infelizmente não tenho acesso ao MCP."},"done":true,\
                "done_reason":"stop","prompt_eval_count":20,"eval_count":8}
                """);
        StringBuilder streamed = new StringBuilder();

        assertThatThrownBy(() -> agentClient.stream(
                externalResearchCommand(), delta -> { }, streamed::append, () -> false))
                .isInstanceOf(ProviderStreamException.class)
                .hasMessage("The model provider could not complete this request");
        assertThat(streamed).isEmpty();
        assertThat(requestBody.get())
                .contains("MANDATORY MCP EXECUTION GATE")
                .doesNotContain("Infelizmente não tenho acesso");
        verify(mcpSession).close();
    }

    @Test
    void keepsProgressiveDiscoveryForLargeAuthorizedCatalogs() {
        List<ToolCallback> callbacks = new ArrayList<>();
        for (int index = 0; index < 11; index++) {
            ToolDefinition definition = mock(ToolDefinition.class);
            when(definition.name()).thenReturn("mcp_bulk_" + index);
            when(definition.description()).thenReturn("Bulk tool " + index);
            when(definition.inputSchema()).thenReturn("{\"type\":\"object\"}");
            ToolCallback callback = mock(ToolCallback.class);
            when(callback.getToolDefinition()).thenReturn(definition);
            callbacks.add(callback);
        }
        McpToolSessionFactory mcpFactory = mock(McpToolSessionFactory.class);
        McpToolSession mcpSession = mock(McpToolSession.class);
        when(mcpSession.callbacks()).thenReturn(callbacks);
        when(mcpSession.evidence()).thenReturn(List.of());
        when(mcpFactory.open(any(), any(), any())).thenReturn(mcpSession);
        SpringAiChatCompletionClient agentClient = new SpringAiChatCompletionClient(
                new SpringAiModelFactory(RestClient.builder(), ObservationRegistry.NOOP),
                new SpringAiMessageMapper(),
                mock(KnowledgeSearchToolFactory.class),
                mock(KnowledgeWriteToolFactory.class),
                mock(AgentPlanToolFactory.class),
                mock(RememberToolFactory.class),
                mcpFactory,
                ObservationRegistry.NOOP);
        serve(STREAM);
        UUID userId = UUID.randomUUID();
        McpRuntimeConnection connection = new McpRuntimeConnection(
                UUID.randomUUID(),
                "Large catalog",
                McpConnectionKind.CUSTOM_REMOTE,
                McpTransportType.STREAMABLE_HTTP,
                null,
                "https://mcp.example.com/mcp",
                List.of(new McpRuntimeTool("bulk", "mcp_bulk_0")));
        ChatCompletionCommand command = new ChatCompletionCommand(
                ProviderType.OLLAMA,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "qwen3:8b",
                List.of(new ChatCompletionMessage("user", "Use a bulk tool")),
                false,
                ConversationMode.AGENT,
                null,
                null,
                new McpToolScope(userId, UUID.randomUUID(), UUID.randomUUID(), List.of(connection)),
                ToolExecutionObserver.NOOP,
                update -> { });

        ChatCompletionOutcome outcome = agentClient.stream(
                command, delta -> { }, delta -> { }, () -> false);

        assertThat(outcome.content()).isEqualTo("Hello");
        assertThat(requestBody.get())
                .contains("\"name\":\"toolSearchTool\"")
                .doesNotContain("\"name\":\"mcp_bulk_0\"");
        verify(mcpSession).close();
    }

    private void serve(String body) {
        server.createContext("/api/chat", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/x-ndjson");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        server.start();
    }

    private ChatCompletionCommand command(boolean thinkingEnabled) {
        return new ChatCompletionCommand(
                ProviderType.OLLAMA,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "qwen3:8b",
                List.of(new ChatCompletionMessage("user", "Hi")),
                thinkingEnabled);
    }

    private ChatCompletionCommand externalResearchCommand() {
        UUID userId = UUID.randomUUID();
        McpRuntimeConnection connection = new McpRuntimeConnection(
                UUID.randomUUID(),
                "Private search",
                McpConnectionKind.CUSTOM_REMOTE,
                McpTransportType.STREAMABLE_HTTP,
                null,
                "https://mcp.example.com/mcp",
                List.of(new McpRuntimeTool("search", "mcp_private_search")));
        return new ChatCompletionCommand(
                ProviderType.OLLAMA,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "granite4.1:8b",
                List.of(
                        new ChatCompletionMessage("system", "Nexo Agent"),
                        new ChatCompletionMessage("user", "Pesquise a cotação do dólar."),
                        new ChatCompletionMessage("assistant", "Infelizmente não tenho acesso ao MCP."),
                        new ChatCompletionMessage("user", "Cara pesquisa direito ai cara")),
                false,
                ConversationMode.AGENT,
                null,
                null,
                new McpToolScope(userId, UUID.randomUUID(), UUID.randomUUID(), List.of(connection)),
                ToolExecutionObserver.NOOP,
                update -> { });
    }

    private SpringAiChatCompletionClient clientWithWorkspaceFactory(
            WorkspaceReadToolFactory workspaceFactory) {
        return new SpringAiChatCompletionClient(
                new SpringAiModelFactory(RestClient.builder(), ObservationRegistry.NOOP),
                new SpringAiMessageMapper(),
                mock(KnowledgeSearchToolFactory.class),
                mock(KnowledgeWriteToolFactory.class),
                mock(AgentPlanToolFactory.class),
                mock(RememberToolFactory.class),
                mock(McpToolSessionFactory.class),
                workspaceFactory,
                ObservationRegistry.NOOP);
    }

    private WorkspaceToolScope workspaceScope() {
        return new WorkspaceToolScope(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Nexo",
                WorkspaceAccessMode.READ_ONLY,
                true);
    }

    private ChatCompletionCommand workspaceCommand(WorkspaceToolScope scope, String request) {
        return new ChatCompletionCommand(
                ProviderType.OLLAMA,
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "granite4.1:8b",
                List.of(new ChatCompletionMessage("user", request)),
                false,
                ConversationMode.AGENT,
                null,
                null,
                null,
                null,
                null,
                scope,
                ToolExecutionObserver.NOOP,
                update -> { });
    }

    private ToolCallback mockCallback(String name, String description) {
        ToolDefinition definition = mock(ToolDefinition.class);
        when(definition.name()).thenReturn(name);
        when(definition.description()).thenReturn(description);
        when(definition.inputSchema()).thenReturn("{\"type\":\"object\"}");
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(definition);
        return callback;
    }

    private record SearchInput(String query) {}
}
