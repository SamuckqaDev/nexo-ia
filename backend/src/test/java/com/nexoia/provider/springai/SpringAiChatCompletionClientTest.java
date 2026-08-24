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
import com.nexoia.provider.dto.ToolExecutionObserver;
import com.nexoia.provider.dto.ToolExecutionStatus;
import com.nexoia.provider.exception.ProviderStreamException;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.model.TokenSource;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    void executesTheGovernedKnowledgeToolThroughSpringAiAndReturnsItsEvidence() {
        RetrievalService retrieval = mock(RetrievalService.class);
        when(retrieval.retrieve(any(), any())).thenReturn(new RetrievalResult(List.of(
                new CitationResponse("Nexo KB", "Principles", 1, "Nexo is truthful.", 0.93))));
        SpringAiChatCompletionClient agentClient = new SpringAiChatCompletionClient(
                new SpringAiModelFactory(RestClient.builder(), ObservationRegistry.NOOP),
                new SpringAiMessageMapper(),
                new KnowledgeSearchToolFactory(retrieval, mock(AuditService.class), Clock.systemUTC()),
                mock(AgentPlanToolFactory.class),
                mock(RememberToolFactory.class),
                mock(McpToolSessionFactory.class),
                ObservationRegistry.NOOP);
        AtomicInteger requests = new AtomicInteger();
        List<String> requestBodies = new ArrayList<>();
        server.createContext("/api/chat", exchange -> {
            requestBodies.add(new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String body = requests.incrementAndGet() == 1
                    ? """
                      {"model":"qwen3:8b","message":{"role":"assistant","content":"",\
                      "tool_calls":[{"id":"call-1","function":{"name":"search_knowledge",\
                      "arguments":{"query":"Nexo identity","limit":2}}}]},"done":true,\
                      "done_reason":"stop","prompt_eval_count":20,"eval_count":1}
                      """
                    : """
                      {"model":"qwen3:8b","message":{"role":"assistant",\
                      "content":"Nexo is truthful."},"done":true,"done_reason":"stop",\
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
                List.of(new ChatCompletionMessage("user", "Who is Nexo?")),
                false,
                ConversationMode.AGENT,
                new KnowledgeToolScope(
                        userId, UUID.randomUUID(), UUID.randomUUID(), List.of(UUID.randomUUID())),
                ToolExecutionObserver.NOOP);

        ChatCompletionOutcome outcome = agentClient.stream(
                agentCommand, delta -> {}, delta -> {}, () -> false);

        assertThat(outcome.content()).isEqualTo("Nexo is truthful.");
        assertThat(outcome.toolExecutions()).hasSize(1);
        assertThat(outcome.toolExecutions().getFirst().citations()).hasSize(1);
        assertThat(requestBodies).hasSize(2);
        assertThat(requestBodies.getFirst()).contains("\"tools\"").contains("search_knowledge");
        assertThat(requestBodies.get(1)).contains("\"role\":\"tool\"");
    }

    @Test
    void executesTheVisiblePlanToolThroughTheSpringAiAdvisorLoop() {
        AtomicReference<AgentPlanUpdate> planUpdate = new AtomicReference<>();
        SpringAiChatCompletionClient agentClient = new SpringAiChatCompletionClient(
                new SpringAiModelFactory(RestClient.builder(), ObservationRegistry.NOOP),
                new SpringAiMessageMapper(),
                mock(KnowledgeSearchToolFactory.class),
                new AgentPlanToolFactory(mock(AuditService.class), Clock.systemUTC()),
                mock(RememberToolFactory.class),
                mock(McpToolSessionFactory.class),
                ObservationRegistry.NOOP);
        AtomicInteger requests = new AtomicInteger();
        List<String> requestBodies = new ArrayList<>();
        server.createContext("/api/chat", exchange -> {
            requestBodies.add(new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String body = requests.incrementAndGet() == 1
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
                new AgentPlanToolScope(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()),
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
        assertThat(requestBodies.getFirst()).contains("update_plan").doesNotContain("userId");
        assertThat(requestBodies.get(1)).contains("\"role\":\"tool\"");
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

        assertThat(outcome.content()).isEqualTo("Hello");
        assertThat(requestBody.get())
                .contains("configured MCP connection could not provide any callable tool")
                .contains("do not claim an external action succeeded");
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
}
