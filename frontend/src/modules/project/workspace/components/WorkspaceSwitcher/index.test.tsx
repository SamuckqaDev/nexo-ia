import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import { useWorkspaceStore } from "../../stores/useWorkspaceStore";
import type { ProjectWorkspace } from "../../types/workspaceTypes";
import { WorkspaceSwitcher } from "./index";

const ownerId = "0c0d3611-301c-4ff4-8656-30a3cf16edbd";
const first: ProjectWorkspace = {
  id: "c3e51f81-4501-42c3-9fe2-0cc38f21976f",
  ownerId,
  name: "Frontend",
  directoryName: "frontend",
  access: "read",
  platform: "linux",
  source: "local-directory",
  addedAt: "2026-08-20T00:00:00Z"
};
const second: ProjectWorkspace = {
  id: "c084be85-503a-486e-84de-1537d70c5484",
  ownerId,
  name: "Backend",
  directoryName: "backend",
  access: "commands",
  platform: "windows",
  source: "local-directory",
  addedAt: "2026-08-20T00:00:00Z"
};

describe("WorkspaceSwitcher", () => {
  beforeEach(() => {
    useWorkspaceStore.setState({ workspaces: [first, second], activeWorkspaceId: first.id });
  });

  it("switches the active workspace without leaving the shell", () => {
    render(<ThemeProvider theme={darkTheme}><WorkspaceSwitcher collapsed={false} onManage={vi.fn()} /></ThemeProvider>);

    fireEvent.change(screen.getByLabelText("Active workspace"), { target: { value: second.id } });

    expect(useWorkspaceStore.getState().activeWorkspaceId).toBe(second.id);
    expect(screen.getByText("backend · Windows")).toBeInTheDocument();
  });
});
