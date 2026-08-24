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

    await expect(listMcpConnections()).rejects.toThrow();
  });
});
