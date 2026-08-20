import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import { useWorkspaceStore } from "../../stores/useWorkspaceStore";
import type { ProjectWorkspace } from "../../types/workspaceTypes";
import { ProjectsPage } from "./index";

const { chooseFolderMock } = vi.hoisted(() => ({ chooseFolderMock: vi.fn() }));

vi.mock("../../hooks/useWorkspaceRegistration", () => ({
  useWorkspaceRegistration: () => ({
    isSupported: true,
    isPicking: false,
    platform: "macos",
    actionLabel: "Choose with Finder",
    error: null,
    chooseFolder: chooseFolderMock
  })
}));

const workspace: ProjectWorkspace = {
  id: "427d6713-f2d4-4b0d-8f72-eaa7f19ebd23",
  ownerId: "0c0d3611-301c-4ff4-8656-30a3cf16edbd",
  name: "nexo-ia",
  directoryName: "nexo-ia",
  access: "read",
  platform: "macos",
  source: "local-directory",
  addedAt: "2026-08-20T00:00:00Z"
};

describe("ProjectsPage", () => {
  beforeEach(() => {
    chooseFolderMock.mockReset();
    chooseFolderMock.mockImplementation(() => {
      useWorkspaceStore.getState().registerWorkspace(workspace);
      return Promise.resolve(workspace);
    });
    useWorkspaceStore.setState({ ownerId: null, workspaces: [], activeWorkspaceId: null, persistenceError: null });
  });

  it("selects a native project folder and makes it the active workspace", async () => {
    const onOpenChat = vi.fn();
    render(<ThemeProvider theme={darkTheme}><ProjectsPage onOpenChat={onOpenChat} /></ThemeProvider>);

    fireEvent.click(screen.getByRole("button", { name: "Choose first folder" }));
    fireEvent.click(screen.getByRole("button", { name: "Choose with Finder" }));

    expect((await screen.findAllByText("Active workspace")).length).toBeGreaterThan(0);
    expect(screen.getAllByText("nexo-ia").length).toBeGreaterThan(0);
    expect(useWorkspaceStore.getState().activeWorkspaceId).not.toBeNull();

    fireEvent.click(screen.getByRole("button", { name: "Open Chat" }));
    expect(onOpenChat).toHaveBeenCalledOnce();
  });
});
