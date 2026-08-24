import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "styled-components";
import { darkTheme } from "../../../../../app/styles/theme";
import { useImageGenerationStore } from "../../../media/stores/useImageGenerationStore";
import { ConversationContextPanel } from "./index";

const listBackendSources = vi.fn();

vi.mock("../../../../knowledge/vault/api/sourceApi", () => ({
  listBackendSources: (vaultId: string): Promise<unknown> => listBackendSources(vaultId),
  registerBackendSource: vi.fn(),
  archiveBackendSource: vi.fn()
}));

const vault = {
  id: "11111111-1111-4111-8111-111111111111",
  name: "Nexo Knowledge Base",
  description: "Product knowledge",
  scope: "PERSONAL" as const,
  workspaceId: null,
  createdAt: "2026-08-21T10:00:00Z",
  updatedAt: "2026-08-21T10:00:00Z"
};

const renderPanel = (
  mode: "chat" | "agent",
  open = true,
  onOpenChange = vi.fn(),
  vaults = [] as typeof vault[],
  selectedVaultIds: string[] = [],
  onToggleVault = vi.fn()
) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={darkTheme}>
        <ConversationContextPanel
          conversationId="conversation-1"
          mode={mode}
          agentPlan={null}
          open={open}
          vaults={vaults}
          selectedVaultIds={selectedVaultIds}
          isVaultSelectionPending={false}
          vaultSelectionError={null}
          onOpenChange={onOpenChange}
          onToggleVault={onToggleVault}
          onManageVaults={vi.fn()}
          onManageWorkspace={vi.fn()}
        />
      </ThemeProvider>
    </QueryClientProvider>
  );
};

describe("ConversationContextPanel", () => {
  beforeEach(() => useImageGenerationStore.getState().reset());

  it("keeps the chat plan honest until Agent mode is selected", () => {
    renderPanel("chat");

    fireEvent.click(screen.getByRole("tab", { name: /plan/i }));
    expect(screen.getByText("No active plan")).toBeInTheDocument();
    expect(screen.getByText(/switch to agent/i)).toBeInTheDocument();
  });

  it("shows the latest real implementation-plan revision", () => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(
      <QueryClientProvider client={queryClient}>
        <ThemeProvider theme={darkTheme}>
          <ConversationContextPanel
            conversationId="conversation-1"
            mode="agent"
            agentPlan={{
              revision: 3,
              explanation: "Implementation advanced",
              steps: [
                { step: "Inspect the current behavior", status: "COMPLETED" },
                { step: "Connect the Plan workspace", status: "IN_PROGRESS" }
              ],
              updatedAt: "2026-08-24T12:00:00Z"
            }}
            open
            vaults={[]}
            selectedVaultIds={[]}
            isVaultSelectionPending={false}
            vaultSelectionError={null}
            onOpenChange={vi.fn()}
            onToggleVault={vi.fn()}
            onManageVaults={vi.fn()}
            onManageWorkspace={vi.fn()}
          />
        </ThemeProvider>
      </QueryClientProvider>
    );

    fireEvent.click(screen.getByRole("tab", { name: /plan/i }));
    expect(screen.getByText("revision 3")).toBeVisible();
    expect(screen.getByText("Inspect the current behavior")).toBeVisible();
    expect(screen.getByText("Connect the Plan workspace")).toBeVisible();
    expect(screen.getByText(/latest persisted revision/i)).toBeVisible();
  });

  it("exposes tasks, artifacts and media as conversation resources", () => {
    renderPanel("chat");

    fireEvent.click(screen.getByRole("tab", { name: /tasks/i }));
    expect(screen.getByText("No tasks yet")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("tab", { name: /artifacts/i }));
    expect(screen.getByText("No artifacts yet")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("tab", { name: /media/i }));
    expect(screen.getByText("No media yet")).toBeInTheDocument();
  });

  it("keeps a navigable resource rail when the workspace is minimized", () => {
    const onOpenChange = vi.fn();
    renderPanel("chat", false, onOpenChange);

    fireEvent.click(screen.getByRole("button", { name: "Media" }));

    expect(onOpenChange).toHaveBeenCalledWith(true);
  });

  it("shows real backend Vault documents and selects the Vault for retrieval", async () => {
    const onToggleVault = vi.fn();
    listBackendSources.mockResolvedValue([{
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
    }]);
    renderPanel("chat", true, vi.fn(), [vault], [], onToggleVault);

    fireEvent.click(screen.getByRole("tab", { name: /vaults/i }));

    expect(await screen.findByText("Nexo Principles")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /use this vault/i }));
    expect(onToggleVault).toHaveBeenCalledWith(vault.id);
  });

  it("shows reported image progress, elapsed time and remaining estimate", () => {
    useImageGenerationStore.getState().upsertJob({
      id: "image-1",
      conversationId: "conversation-1",
      prompt: "A cyan architecture diagram",
      status: "GENERATING",
      progress: 42,
      etaSeconds: 18,
      startedAt: new Date().toISOString(),
      errorMessage: null
    });
    renderPanel("chat");

    fireEvent.click(screen.getByRole("tab", { name: /media/i }));

    expect(screen.getByRole("progressbar", { name: /image generation progress/i })).toHaveValue(42);
    expect(screen.getByText("42%")).toBeVisible();
    expect(screen.getByText(/about 18s remaining/i)).toBeVisible();
  });
});
