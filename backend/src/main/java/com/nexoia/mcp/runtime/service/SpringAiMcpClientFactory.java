package com.nexoia.mcp.runtime.service;

import com.nexoia.mcp.connection.exception.McpConnectionUnavailableException;
import com.nexoia.mcp.connection.model.McpConnectionKind;
import com.nexoia.mcp.runtime.dto.McpConnectionSnapshot;
import com.nexoia.mcp.runtime.dto.McpDiscoveredTool;
import com.nexoia.mcp.runtime.dto.McpRuntimeConnection;
import com.nexoia.mcp.runtime.dto.McpRuntimeTool;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/** Opens short-lived, request-owned MCP clients through the official Java SDK and Spring AI adapter. */
@Slf4j
@Service
public class SpringAiMcpClientFactory implements McpClientFactory {

    private static final int MAX_TOOL_PAGES = 5;
    private static final int MAX_TOOLS = 100;
    private final String dockerExecutable;
    private final Duration requestTimeout;
    private final McpJsonMapper jsonMapper = new JacksonMcpJsonMapper(JsonMapper.builder().build());

    public SpringAiMcpClientFactory(
            @Value("${nexo.mcp.docker-command:docker}") String dockerExecutable,
            @Value("${nexo.mcp.request-timeout:20s}") Duration requestTimeout) {
        this.dockerExecutable = dockerExecutable;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public McpClientSession open(McpRuntimeConnection connection) {
        McpSyncClient client = null;
        try {
            client = McpClient.sync(transport(connection))
                    .clientInfo(new McpSchema.Implementation("nexo-ia", "0.1.0"))
                    .initializationTimeout(requestTimeout)
                    .requestTimeout(requestTimeout)
                    .build();
            McpSchema.InitializeResult initialized = client.initialize();
            List<McpSchema.Tool> discovered = tools(client);
            Map<String, String> exposedNames = new HashMap<>();
            for (McpRuntimeTool tool : connection.enabledTools()) {
                exposedNames.put(tool.externalName(), tool.exposedName());
            }
            McpSyncClient connectedClient = client;
            List<ToolCallback> callbacks = discovered.stream()
                    .filter(tool -> exposedNames.containsKey(tool.name()))
                    .map(tool -> (ToolCallback) SyncMcpToolCallback.builder()
                            .mcpClient(connectedClient)
                            .tool(tool)
                            .prefixedToolName(exposedNames.get(tool.name()))
                            .build())
                    .toList();
            McpSchema.Implementation server = initialized.serverInfo();
            return new McpClientSession(
                    client,
                    new McpConnectionSnapshot(
                            server == null ? connection.displayName() : server.name(),
                            server == null ? null : server.version(),
                            discovered.stream().map(this::discoveredTool).toList()),
                    callbacks);
        } catch (RuntimeException exception) {
            if (client != null) {
                client.closeGracefully();
            }
            log.warn("[NEXO-BACK][MCP] Connection failed connectionId={} kind={} reason={}",
                    connection.id(), connection.connectionKind(), exception.getClass().getSimpleName());
            throw new McpConnectionUnavailableException(exception);
        }
    }

    private McpClientTransport transport(McpRuntimeConnection connection) {
        if (connection.connectionKind() == McpConnectionKind.DOCKER_CATALOG) {
            ServerParameters parameters = ServerParameters.builder(dockerExecutable)
                    .args("mcp", "gateway", "run", "--servers", connection.catalogServerId())
                    .build();
            StdioClientTransport transport = new StdioClientTransport(parameters, jsonMapper);
            transport.setStdErrorHandler(line -> log.debug("[NEXO-BACK][MCP] Docker gateway: {}",
                    line.length() > 300 ? line.substring(0, 300) : line));
            return transport;
        }

        URI endpoint = URI.create(connection.endpoint());
        String baseUrl = endpoint.getScheme() + "://" + endpoint.getAuthority();
        String path = endpoint.getRawPath();
        return HttpClientStreamableHttpTransport.builder(baseUrl)
                .endpoint(path == null || path.isBlank() ? "/mcp" : path)
                .connectTimeout(requestTimeout)
                .build();
    }

    private List<McpSchema.Tool> tools(McpSyncClient client) {
        List<McpSchema.Tool> tools = new ArrayList<>();
        McpSchema.ListToolsResult page = client.listTools();
        int pages = 0;
        while (page != null && pages++ < MAX_TOOL_PAGES && tools.size() < MAX_TOOLS) {
            page.tools().stream().limit(MAX_TOOLS - tools.size()).forEach(tools::add);
            if (page.nextCursor() == null || page.nextCursor().isBlank()) {
                break;
            }
            page = client.listTools(page.nextCursor());
        }
        return List.copyOf(tools);
    }

    private McpDiscoveredTool discoveredTool(McpSchema.Tool tool) {
        McpSchema.ToolAnnotations annotations = tool.annotations();
        return new McpDiscoveredTool(
                tool.name(),
                tool.title(),
                bounded(tool.description(), 2000),
                tool.inputSchema() == null ? Map.of() : new LinkedHashMap<>(tool.inputSchema()),
                annotations == null ? null : annotations.readOnlyHint(),
                annotations == null ? null : annotations.destructiveHint(),
                annotations == null ? null : annotations.openWorldHint());
    }

    private String bounded(String value, int maximum) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maximum ? trimmed : trimmed.substring(0, maximum);
    }
}
