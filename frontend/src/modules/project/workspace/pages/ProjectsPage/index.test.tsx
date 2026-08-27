import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import type { ServerWorkspace } from "../../types/serverWorkspaceTypes";
import { ProjectsPage } from "./index";

const { refreshMock, workspace } = vi.hoisted(() => ({
  refreshMock: vi.fn(),
  workspace: {
    id: "427d6713-f2d4-4b0d-8f72-eaa7f19ebd23",
    name: "nexo-ia",
    storageType: "MOUNTED",
    accessMode: "READ_ONLY",
    status: "AVAILABLE",
    relativePath: "projects/nexo-ia",
    lastScannedAt: "2026-08-20T00:00:00Z",
    createdAt: "2026-08-20T00:00:00Z",
    updatedAt: "2026-08-20T00:00:00Z"
  } satisfies ServerWorkspace
}));

vi.mock("../../hooks/useServerWorkspaces", () => ({
  useServerWorkspaces: () => ({ data: [workspace], isLoading: false, isError: false }),
  useServerWorkspaceStatus: () => ({
    data: {
      status: "AVAILABLE",
      storageType: "MOUNTED",
      accessMode: "READ_ONLY",
      relativePath: "projects/nexo-ia",
      structureFingerprint: "abc",
      lastScannedAt: "2026-08-20T00:00:00Z",
      git: { branch: "main", head: "abc", detached: false },
      detectedStack: ["maven"],
      reason: null
    },
    isLoading: false,
    isError: false
  }),
  useCreateServerWorkspace: () => ({ mutate: vi.fn(), isPending: false, isError: false, error: null }),
  useDeleteServerWorkspace: () => ({ mutate: vi.fn(), isPending: false, isError: false, error: null }),
  useRefreshServerWorkspace: () => ({ mutate: refreshMock, isPending: false }),
  useWorkspaceBindings: () => ({ data: [], isLoading: false, isError: false, refetch: vi.fn() })
}));

vi.mock("../../hooks/useLocalWorkspacePicker", () => ({
  useLocalWorkspacePicker: () => ({
    available: false,
    pending: false,
    error: null,
    chooseLocalWorkspace: vi.fn()
  })
}));

vi.mock("../../../../device/runtime/components/DesktopRuntimeCard", () => ({
  DesktopRuntimeCard: () => <div>desktop runtime</div>
}));

vi.mock("../../components/ServerWorkspaceTree", () => ({
  ServerWorkspaceTree: () => <div>server tree</div>
}));

describe("ProjectsPage", () => {
  beforeEach(() => refreshMock.mockReset());

  it("shows the server workspace and opens Chat", async () => {
    const onOpenChat = vi.fn();
    render(<ThemeProvider theme={darkTheme}><ProjectsPage onOpenChat={onOpenChat} /></ThemeProvider>);

    expect(await screen.findAllByText("nexo-ia")).not.toHaveLength(0);
    expect(screen.getByText("server tree")).toBeVisible();

    fireEvent.click(screen.getAllByRole("button", { name: "Open Chat" })[0]);
    expect(onOpenChat).toHaveBeenCalledOnce();
  });

  it("refreshes the live server structure", async () => {
    render(<ThemeProvider theme={darkTheme}><ProjectsPage onOpenChat={vi.fn()} /></ThemeProvider>);

    fireEvent.click(await screen.findByRole("button", { name: "Refresh" }));
    expect(refreshMock).toHaveBeenCalledWith(workspace.id);
  });
});
