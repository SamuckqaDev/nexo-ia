import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import { useVaultCatalogStore } from "../../stores/useVaultCatalogStore";
import { VaultsPage } from "./index";

vi.mock("../../api/knowledgeWorkspaceApi", () => ({
  listKnowledgeWorkspaces: vi.fn().mockResolvedValue([]),
  createKnowledgeWorkspace: vi.fn()
}));

const renderVaultsPage = (): ReturnType<typeof render> => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={darkTheme}><VaultsPage /></ThemeProvider>
    </QueryClientProvider>
  );
};

describe("VaultsPage", () => {
  beforeEach(() => {
    useVaultCatalogStore.getState().reset();
    useVaultCatalogStore.getState().initialize("00000000-0000-4000-8000-000000000101");
  });

  it("creates a selectable session Vault draft", async () => {
    renderVaultsPage();

    fireEvent.click(screen.getByRole("button", { name: "New Vault" }));
    fireEvent.change(screen.getByLabelText("Vault name"), { target: { value: "Architecture decisions" } });
    fireEvent.change(screen.getByLabelText("Purpose"), { target: { value: "Ground answers in accepted architecture decisions." } });
    fireEvent.click(screen.getByRole("button", { name: "Create draft" }));

    expect(await screen.findByRole("heading", { name: "Architecture decisions" })).toBeInTheDocument();
    expect(screen.getByText("This Vault has no sources")).toBeInTheDocument();
  });

  it("opens source knowledge and attaches readable content to Chat", () => {
    renderVaultsPage();

    fireEvent.click(screen.getByRole("button", { name: /Nexo product docs/i }));
    fireEvent.click(screen.getByRole("button", { name: /PRODUCT_VISION.md/i }));
    expect(screen.getByText(/local-first, team-ready AI workspace/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Attach to Chat" }));
    expect(screen.getByRole("button", { name: "Attached to Chat" })).toBeInTheDocument();
  });

  it("exposes an accessible map whose nodes open the matching knowledge", () => {
    renderVaultsPage();

    fireEvent.click(screen.getByRole("button", { name: "Knowledge workbench" }));
    expect(screen.getByLabelText(/Interactive knowledge map/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /^Source: CONTEXT_AND_SKILL_GOVERNANCE.md/i }));

    expect(screen.getByText(/Explicit Skill invocation does not bypass identity/i)).toBeInTheDocument();
  });

  it("opens the knowledge map in a movable workbench window", () => {
    renderVaultsPage();

    fireEvent.click(screen.getByRole("button", { name: "Knowledge workbench" }));

    expect(screen.getByRole("dialog", { name: "Knowledge Workbench" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Maximize workbench" }));
    expect(screen.getByRole("button", { name: "Restore workbench" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Close knowledge workbench" }));
    expect(screen.queryByRole("dialog", { name: "Knowledge Workbench" })).not.toBeInTheDocument();
  });
});
