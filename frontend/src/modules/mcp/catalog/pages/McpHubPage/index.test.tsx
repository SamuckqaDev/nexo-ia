import { render, screen } from "@testing-library/react";
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
});
