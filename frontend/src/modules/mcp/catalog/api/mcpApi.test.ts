import { afterEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "../../../../shared/api/client";
import { getMcpCatalog, listMcpConnections } from "./mcpApi";

afterEach(() => vi.restoreAllMocks());

describe("MCP API", () => {
  it("parses Docker catalog cost and safety metadata", async () => {
    vi.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        code: 200,
        message: "MCP catalog retrieved",
        data: [{
          dockerAvailable: true,
          gatewayVersion: "v0.43.3",
          source: "mcp/docker-mcp-catalog",
          refreshedAt: "2026-08-24T12:00:00Z",
          servers: [{
            id: "fetch",
            title: "Fetch",
            description: "Fetch a public page",
            category: "web",
            image: "mcp/fetch",
            iconUrl: null,
            license: "MIT",
            costType: "LOCAL_FREE",
            riskLevel: "READ_ONLY",
            requiresSecrets: false,
            requiresConfiguration: false,
            toolCount: 1,
            recommended: true
          }]
        }]
      }
    });

    const catalog = await getMcpCatalog();

    expect(catalog.servers[0]).toMatchObject({ id: "fetch", costType: "LOCAL_FREE" });
  });

  it("rejects a malformed connection snapshot", async () => {
    vi.spyOn(apiClient, "get").mockResolvedValue({
      data: { code: 200, message: "MCP connections retrieved", data: [{ id: "not-a-uuid" }] }
    });

    await expect(listMcpConnections()).rejects.toThrow(
      "Nexo received an incompatible MCP connection response"
    );
  });

  it("parses the backend external tool-name contract", async () => {
    vi.spyOn(apiClient, "get").mockResolvedValue({
      data: {
        code: 200,
        message: "MCP connections retrieved",
        data: [{
          id: "11111111-1111-4111-8111-111111111111",
          displayName: "Fetch",
          connectionKind: "DOCKER_CATALOG",
          transportType: "DOCKER_GATEWAY",
          catalogServerId: "fetch",
          endpoint: null,
          costType: "LOCAL_FREE",
          status: "CONNECTED",
          enabled: false,
          serverName: "fetch",
          serverVersion: null,
          lastErrorCode: null,
          lastConnectedAt: "2026-08-24T12:00:00Z",
          tools: [{
            externalName: "fetch",
            exposedName: "mcp_12345678_fetch",
            title: "Fetch",
            description: "Fetch a public page",
            enabled: false,
            readOnlyHint: true,
            destructiveHint: false,
            openWorldHint: false,
            discoveredAt: "2026-08-24T12:00:00Z"
          }],
          createdAt: "2026-08-24T12:00:00Z",
          updatedAt: "2026-08-24T12:00:00Z"
        }]
      }
    });

    const connections = await listMcpConnections();

    expect(connections[0].tools[0].externalName).toBe("fetch");
  });
});
