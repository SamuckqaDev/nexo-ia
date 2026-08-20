import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import { useWorkspaceStore } from "../../stores/useWorkspaceStore";
import { ProjectsPage } from "./index";

describe("ProjectsPage", () => {
  beforeEach(() => {
    useWorkspaceStore.setState({ workspaces: [], activeWorkspaceId: null });
  });

  it("adds a project folder and makes it the active workspace", async () => {
    const onOpenChat = vi.fn();
    render(<ThemeProvider theme={darkTheme}><ProjectsPage onOpenChat={onOpenChat} /></ThemeProvider>);

    fireEvent.change(screen.getByLabelText("Workspace name"), { target: { value: "Nexo IA" } });
    fireEvent.change(screen.getByLabelText("Project folder"), { target: { value: "/projects/nexo-ia" } });
    fireEvent.click(screen.getByRole("button", { name: "Add and select" }));

    expect((await screen.findAllByText("Active workspace")).length).toBeGreaterThan(0);
    expect(screen.getAllByText("Nexo IA").length).toBeGreaterThan(0);
    expect(useWorkspaceStore.getState().activeWorkspaceId).not.toBeNull();

    fireEvent.click(screen.getByRole("button", { name: "Open Chat" }));
    expect(onOpenChat).toHaveBeenCalledOnce();
  });
});
