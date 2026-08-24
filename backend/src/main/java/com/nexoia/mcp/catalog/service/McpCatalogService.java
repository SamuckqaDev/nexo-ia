package com.nexoia.mcp.catalog.service;

import com.nexoia.mcp.catalog.dto.McpCatalogResponse;
import com.nexoia.mcp.catalog.dto.McpCatalogServerResponse;
import com.nexoia.mcp.catalog.dto.McpRiskLevel;
import com.nexoia.mcp.connection.exception.McpConnectionNotFoundException;
import com.nexoia.mcp.connection.model.McpCostType;
import com.nexoia.mcp.gateway.service.DockerMcpGatewayRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Reads Docker's live MCP Catalog and falls back to a small reviewed free-first index. */
@Service
@RequiredArgsConstructor
public class McpCatalogService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final String CATALOG_REFERENCE = "mcp/docker-mcp-catalog";
    private static final List<String> RECOMMENDED = List.of("fetch", "duckduckgo", "git", "playwright");

    private final DockerMcpCommandRunner commands;
    private final DockerMcpGatewayRegistry gateways;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private volatile McpCatalogResponse cached;

    public McpCatalogResponse catalog() {
        McpCatalogResponse current = cached;
        Instant now = clock.instant();
        if (current != null && current.refreshedAt().plus(CACHE_TTL).isAfter(now)) {
            return current;
        }
        synchronized (this) {
            current = cached;
            if (current == null || !current.refreshedAt().plus(CACHE_TTL).isAfter(now)) {
                cached = load(now);
            }
            return cached;
        }
    }

    public McpCatalogServerResponse server(String serverId) {
        return catalog().servers().stream()
                .filter(server -> server.id().equals(serverId))
                .findFirst()
                .orElseThrow(McpConnectionNotFoundException::new);
    }

    private McpCatalogResponse load(Instant refreshedAt) {
        DockerMcpCommandResult version = commands.run(
                List.of("mcp", "version"), Duration.ofSeconds(5));
        DockerMcpCommandResult catalog = commands.run(
                List.of("mcp", "catalog", "server", "ls", CATALOG_REFERENCE, "--format", "json"),
                Duration.ofSeconds(15));
        if (version.exitCode() != 0 || catalog.exitCode() != 0 || catalog.output().isBlank()) {
            if (gateways.available()) {
                List<McpCatalogServerResponse> available = fallback().stream()
                        .filter(server -> gateways.endpoint(server.id()).isPresent())
                        .toList();
                return new McpCatalogResponse(
                        !available.isEmpty(), "sidecar", "docker-sidecars", refreshedAt, available);
            }
            return new McpCatalogResponse(false, null, "reviewed-fallback", refreshedAt, fallback());
        }

        try {
            JsonNode root = objectMapper.readTree(catalog.output());
            List<McpCatalogServerResponse> servers = new ArrayList<>();
            for (JsonNode entry : root.path("servers")) {
                JsonNode server = entry.path("snapshot").path("server");
                String id = text(server, "name");
                if (id == null || !id.matches("[a-z0-9][a-z0-9._-]*")) {
                    continue;
                }
                boolean requiresSecrets = populated(server.path("secrets")) || populated(server.path("oauth"));
                boolean requiresConfiguration = populated(server.path("config"));
                String image = text(server, "image");
                String license = text(server.path("metadata"), "license");
                McpCostType costType = costType(image, license, requiresSecrets);
                servers.add(new McpCatalogServerResponse(
                        id,
                        defaultText(text(server, "title"), id),
                        defaultText(text(server, "description"), "No catalog description provided."),
                        defaultText(text(server.path("metadata"), "category"), "other"),
                        image,
                        text(server, "icon"),
                        license,
                        costType,
                        risk(id),
                        requiresSecrets,
                        requiresConfiguration,
                        server.path("tools").isArray() ? server.path("tools").size() : 0,
                        RECOMMENDED.contains(id)));
            }
            servers.sort(catalogOrder());
            return new McpCatalogResponse(
                    true, version.output().trim(), CATALOG_REFERENCE, refreshedAt, List.copyOf(servers));
        } catch (RuntimeException exception) {
            return new McpCatalogResponse(false, version.output().trim(), "reviewed-fallback", refreshedAt, fallback());
        }
    }

    private Comparator<McpCatalogServerResponse> catalogOrder() {
        return Comparator
                .comparing(McpCatalogServerResponse::recommended).reversed()
                .thenComparingInt(server -> costRank(server.costType()))
                .thenComparing(McpCatalogServerResponse::title, String.CASE_INSENSITIVE_ORDER);
    }

    private int costRank(McpCostType value) {
        return switch (value) {
            case LOCAL_FREE -> 0;
            case FREE_TIER -> 1;
            case ACCOUNT_REQUIRED -> 2;
            case UNKNOWN -> 3;
            case PAID -> 4;
        };
    }

    private McpCostType costType(String image, String license, boolean requiresSecrets) {
        if (requiresSecrets) {
            return McpCostType.ACCOUNT_REQUIRED;
        }
        if (image != null && !image.isBlank() && license != null && !license.isBlank()) {
            return McpCostType.LOCAL_FREE;
        }
        return McpCostType.UNKNOWN;
    }

    private McpRiskLevel risk(String id) {
        return switch (id) {
            case "fetch", "duckduckgo" -> McpRiskLevel.READ_ONLY;
            case "git", "playwright", "github-official" -> McpRiskLevel.READ_WRITE;
            default -> McpRiskLevel.UNKNOWN;
        };
    }

    private boolean populated(JsonNode value) {
        return !value.isMissingNode() && !value.isNull()
                && (!(value.isArray() || value.isObject()) || !value.isEmpty());
    }

    private String text(JsonNode value, String field) {
        JsonNode node = value.path(field);
        return node.isTextual() && !node.asText().isBlank() ? node.asText().trim() : null;
    }

    private String defaultText(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private List<McpCatalogServerResponse> fallback() {
        return List.of(
                fallbackServer("fetch", "Fetch", "Fetch and extract public web pages.",
                        "mcp/fetch", McpRiskLevel.READ_ONLY, 1),
                fallbackServer("duckduckgo", "Private Web Search", "Search the web without an API key.",
                        "mcp/duckduckgo", McpRiskLevel.READ_ONLY, 2),
                fallbackServer("git", "Git", "Inspect and operate explicitly mounted Git repositories.",
                        "mcp/git", McpRiskLevel.READ_WRITE, 12),
                fallbackServer("playwright", "Playwright", "Automate a sandboxed browser.",
                        "mcp/playwright", McpRiskLevel.READ_WRITE, 23));
    }

    private McpCatalogServerResponse fallbackServer(
            String id, String title, String description, String image, McpRiskLevel risk, int toolCount) {
        return new McpCatalogServerResponse(
                id, title, description, "developer-tools", image, null, null,
                McpCostType.LOCAL_FREE, risk, false, "git".equals(id), toolCount, true);
    }
}
