import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import { useMcpHub } from "../../hooks/useMcpHub";
import type { McpHubResult } from "../../types/mcpTypes";
import { McpHubPage } from "./index";

vi.mock("../../hooks/useMcpHub", () => ({ useMcpHub: vi.fn() }));

const mutation = (): Record<string, unknown> => ({
  isPending: false,
  variables: undefined,
  mutate: vi.fn()
});

describe("McpHubPage", () => {
  beforeEach(() => {
    vi.mocked(useMcpHub).mockReturnValue({
      catalog: {
        data: {
          dockerAvailable: false,
          gatewayVersion: null,
          source: "reviewed-fallback",
          refreshedAt: "2026-08-24T12:00:00Z",
          servers: [{
            id: "fetch",
            title: "Fetch",
            description: "Fetch public pages",
            category: "web",
            image: "mcp/fetch",
            iconUrl: null,
            license: null,
            costType: "LOCAL_FREE",
            riskLevel: "READ_ONLY",
            requiresSecrets: false,
            requiresConfiguration: false,
            toolCount: 1,
            recommended: true
          }]
        },
        isLoading: false,
        isError: false,
        error: null
      },
      connections: { data: [], isLoading: false, isError: false, error: null },
      installDocker: mutation(),
      createRemote: mutation(),
      discover: mutation(),
      selectTools: mutation(),
      setEnabled: mutation(),
      remove: mutation()
    } as unknown as McpHubResult);
  });

  it("explains an unavailable Docker runtime without presenting it as pending", () => {
    render(
      <ThemeProvider theme={darkTheme}>
        <McpHubPage />
      </ThemeProvider>
    );

    expect(screen.getByText(/Docker MCP is not connected to this Nexo runtime/i)).toBeVisible();
    const unavailable = screen.getByRole("button", { name: "Docker runtime unavailable" });
    expect(unavailable).toBeDisabled();
    expect(unavailable).toHaveAttribute("aria-busy", "false");
    expect(unavailable).toHaveStyle({ cursor: "not-allowed" });
    expect(screen.getByRole("button", { name: "Connect custom" })).toBeEnabled();
    expect(screen.getByText(/public HTTPS Streamable HTTP endpoint/i)).toBeVisible();
  });

  it("makes the separate Agent activation explicit for an allowed tool", async () => {
    const setEnabled = mutation();
    vi.mocked(useMcpHub).mockReturnValue({
      catalog: {
        data: {
          dockerAvailable: true,
          gatewayVersion: "sidecar",
          source: "docker",
          refreshedAt: "2026-08-24T12:00:00Z",
          servers: []
        },
        isLoading: false,
        isError: false,
        error: null
      },
      connections: {
        data: [{
          id: "23ab2ec1-9fc5-4dd9-a18c-6cc8b62130c5",
          displayName: "Fetch",
          connectionKind: "DOCKER_CATALOG",
          transportType: "DOCKER_GATEWAY",
          catalogServerId: "fetch",
          endpoint: null,
          costType: "LOCAL_FREE",
          status: "CONNECTED",
          enabled: false,
          serverName: "Docker AI MCP Gateway",
          serverVersion: "2.0.1",
          lastErrorCode: null,
          lastConnectedAt: "2026-08-24T12:00:00Z",
          tools: [{
            externalName: "fetch",
            exposedName: "mcp_fetch_fetch",
            title: "fetch",
            description: "Fetch a public URL",
            enabled: true,
            readOnlyHint: true,
            destructiveHint: false,
            openWorldHint: true,
            discoveredAt: "2026-08-24T12:00:00Z"
          }],
          createdAt: "2026-08-24T12:00:00Z",
          updatedAt: "2026-08-24T12:00:00Z"
        }],
        isLoading: false,
        isError: false,
        error: null
      },
      installDocker: mutation(),
      createRemote: mutation(),
      discover: mutation(),
      selectTools: mutation(),
      setEnabled,
      remove: mutation()
    } as unknown as McpHubResult);

    render(
      <ThemeProvider theme={darkTheme}>
        <McpHubPage />
      </ThemeProvider>
    );

    expect((await screen.findAllByText("Ready · Off in Agent")).length).toBeGreaterThan(0);
    expect(screen.getByText(/1 allowed tool is selected.*still off in Agent/i)).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Enable 1 tool in Agent" }));
    expect(setEnabled.mutate).toHaveBeenCalledWith({
      id: "23ab2ec1-9fc5-4dd9-a18c-6cc8b62130c5",
      enabled: true
    });
  });

  it("keeps the machine profile as one model-selected catalog", async () => {
    const selectTools = mutation();
    const setEnabled = mutation();
    vi.mocked(useMcpHub).mockReturnValue({
      catalog: {
        data: {
          dockerAvailable: true,
          gatewayVersion: "sidecar",
          source: "docker",
          refreshedAt: "2026-09-11T12:00:00Z",
          servers: []
        },
        isLoading: false,
        isError: false,
        error: null
      },
      connections: {
        data: [{
          id: "68c20cf2-1f1e-4c6b-a4ed-f4588921d1e4",
          displayName: "Machine MCP profile",
          connectionKind: "DOCKER_CATALOG",
          transportType: "DOCKER_GATEWAY",
          catalogServerId: "docker-profile",
          endpoint: null,
          costType: "LOCAL_FREE",
          status: "CONNECTED",
          enabled: true,
          serverName: "Docker AI MCP Gateway",
          serverVersion: "2.0.1",
          lastErrorCode: null,
          lastConnectedAt: "2026-09-11T12:00:00Z",
          tools: [{
            externalName: "fetch",
            exposedName: "mcp_profile_fetch",
            title: "Fetch",
            description: "Fetch a public URL",
            enabled: false,
            readOnlyHint: true,
            destructiveHint: false,
            openWorldHint: true,
            discoveredAt: "2026-09-11T12:00:00Z"
          }],
          createdAt: "2026-09-11T12:00:00Z",
          updatedAt: "2026-09-11T12:00:00Z"
        }],
        isLoading: false,
        isError: false,
        error: null
      },
      installDocker: mutation(),
      createRemote: mutation(),
      discover: mutation(),
      selectTools,
      setEnabled,
      remove: mutation()
    } as unknown as McpHubResult);

    render(
      <ThemeProvider theme={darkTheme}>
        <McpHubPage />
      </ThemeProvider>
    );

    expect(await screen.findByText(/All 1 safe profile tools stay together as one catalog/i)).toBeVisible();
    expect(screen.queryByRole("button", { name: "Save allowed tools" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Fetch, available to Agent" })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Disable catalog in Agent" }));
    expect(setEnabled.mutate).toHaveBeenCalledWith({
      id: "68c20cf2-1f1e-4c6b-a4ed-f4588921d1e4",
      enabled: false
    });
    expect(selectTools.mutate).not.toHaveBeenCalled();
  });
});
