import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { describe, expect, it, vi } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import { VaultsPage } from "./index";

const listBackendVaults = vi.fn();
const listBackendSources = vi.fn();
const listBackendKnowledgeGraph = vi.fn();

vi.mock("../../api/vaultApi", () => ({
  listBackendVaults: (): Promise<unknown> => listBackendVaults(),
  createBackendVault: vi.fn(),
  updateBackendVault: vi.fn(),
  archiveBackendVault: vi.fn()
}));

vi.mock("../../api/sourceApi", () => ({
  listBackendSources: (): Promise<unknown> => listBackendSources(),
  registerBackendSource: vi.fn(),
  archiveBackendSource: vi.fn()
}));

vi.mock("../../api/knowledgeGraphApi", () => ({
  listBackendKnowledgeGraph: (): Promise<unknown> => listBackendKnowledgeGraph()
}));

const vault = {
  id: "11111111-1111-4111-8111-111111111111",
  name: "Nexo Knowledge Base",
  description: "Seeded docs",
  scope: "PERSONAL",
  workspaceId: null,
  createdAt: "2026-08-21T10:00:00Z",
  updatedAt: "2026-08-21T10:00:00Z"
};

const source = {
  id: "22222222-2222-4222-8222-222222222222",
  vaultId: vault.id,
  sourceKind: "UPLOAD",
  displayName: "Nexo Principles",
  mimeType: "text/markdown",
  byteSize: 2048,
  status: "READY",
  errorCode: null,
  createdAt: "2026-08-21T10:00:00Z",
  updatedAt: "2026-08-21T10:00:00Z"
};

const renderPage = (): ReturnType<typeof render> => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={darkTheme}><VaultsPage /></ThemeProvider>
    </QueryClientProvider>
  );
};

describe("VaultsPage", () => {
  it("shows an empty state when the backend has no Vaults", async () => {
    listBackendVaults.mockResolvedValueOnce([]);
    renderPage();

    expect(await screen.findByText("No Vaults yet")).toBeInTheDocument();
  });

  it("lists real backend Vaults and their embedded sources", async () => {
    listBackendVaults.mockResolvedValue([vault]);
    listBackendSources.mockResolvedValue([source]);
    renderPage();

    fireEvent.click(await screen.findByRole("button", { name: /Nexo Knowledge Base/i }));

    expect(await screen.findByText("Nexo Principles")).toBeInTheDocument();
    expect(screen.getByText("Ready")).toBeInTheDocument();
  });

  it("opens the create-Vault form", async () => {
    listBackendVaults.mockResolvedValue([]);
    renderPage();

    fireEvent.click(await screen.findByRole("button", { name: "New Vault" }));

    expect(screen.getByRole("heading", { name: "Create a Knowledge Vault" })).toBeInTheDocument();
  });

  it("opens the semantic workbench with the authenticated backend graph", async () => {
    listBackendVaults.mockResolvedValue([vault]);
    listBackendSources.mockResolvedValue([source]);
    listBackendKnowledgeGraph.mockResolvedValue({
      nodes: [
        { id: `vault:${vault.id}`, kind: "VAULT", vaultId: vault.id, sourceId: null, ordinal: null, label: vault.name, detail: "1 source", excerpt: vault.description, status: "PERSONAL" },
        { id: `source:${source.id}`, kind: "SOURCE", vaultId: vault.id, sourceId: source.id, ordinal: null, label: source.displayName, detail: "0 chunks", excerpt: null, status: "READY" }
      ],
      edges: [
        { id: "contains:test", relation: "CONTAINS", fromId: `vault:${vault.id}`, toId: `source:${source.id}`, similarity: null }
      ],
      vaultCount: 1,
      sourceCount: 1,
      chunkCount: 0,
      truncated: false
    });
    renderPage();

    fireEvent.click(await screen.findByRole("button", { name: "Knowledge graph" }));

    expect(await screen.findByRole("dialog", { name: "Semantic Knowledge Workbench" })).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: /vault: Nexo Knowledge Base/i })).toBeInTheDocument();
  });
});
