import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import { useWorkspaceStore } from "../../stores/useWorkspaceStore";
import type { ProjectWorkspace } from "../../types/workspaceTypes";
import { WorkspaceSwitcher } from "./index";

const first: ProjectWorkspace = { id: "one", name: "Frontend", path: "/projects/frontend", access: "read", addedAt: "2026-08-20T00:00:00Z" };
const second: ProjectWorkspace = { id: "two", name: "Backend", path: "/projects/backend", access: "commands", addedAt: "2026-08-20T00:00:00Z" };

describe("WorkspaceSwitcher", () => {
  beforeEach(() => {
    useWorkspaceStore.setState({ workspaces: [first, second], activeWorkspaceId: first.id });
  });

  it("switches the active workspace without leaving the shell", () => {
    render(<ThemeProvider theme={darkTheme}><WorkspaceSwitcher collapsed={false} onManage={vi.fn()} /></ThemeProvider>);

    fireEvent.change(screen.getByLabelText("Active workspace"), { target: { value: second.id } });

    expect(useWorkspaceStore.getState().activeWorkspaceId).toBe(second.id);
    expect(screen.getByText(second.path)).toBeInTheDocument();
  });
});
