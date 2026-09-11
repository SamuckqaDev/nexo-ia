import { describe, expect, it } from "vitest";
import type { McpConnection } from "../types/mcpTypes";
import { countAgentTools, isMachineProfile } from "./mcpConnectionService";

const connection = (catalogServerId: string | null, toolStates: boolean[]): McpConnection => ({
  id: "68c20cf2-1f1e-4c6b-a4ed-f4588921d1e4",
  displayName: "MCP connection",
  connectionKind: "DOCKER_CATALOG",
  transportType: "DOCKER_GATEWAY",
  catalogServerId,
  endpoint: null,
  costType: "LOCAL_FREE",
  status: "CONNECTED",
  enabled: true,
  serverName: "Docker AI MCP Gateway",
  serverVersion: "2.0.1",
  lastErrorCode: null,
  lastConnectedAt: "2026-09-11T12:00:00Z",
  tools: toolStates.map((enabled: boolean, index: number) => ({
    externalName: `tool_${index}`,
    exposedName: `mcp_profile_tool_${index}`,
    title: `Tool ${index}`,
    description: "A discovered MCP tool",
    enabled,
    readOnlyHint: true,
    destructiveHint: false,
    openWorldHint: true,
    discoveredAt: "2026-09-11T12:00:00Z"
  })),
  createdAt: "2026-09-11T12:00:00Z",
  updatedAt: "2026-09-11T12:00:00Z"
});

describe("mcpConnectionService", () => {
  it("counts the complete machine profile catalog despite legacy tool flags", () => {
    const profile = connection("docker-profile", [false, false, false]);

    expect(isMachineProfile(profile)).toBe(true);
    expect(countAgentTools(profile)).toBe(3);
  });

  it("counts only explicitly selected tools on an ordinary connection", () => {
    expect(countAgentTools(connection("fetch", [true, false, true]))).toBe(2);
  });
});
