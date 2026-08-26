import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { describe, expect, it, vi } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import { WorkspaceSwitcher } from "./index";

vi.mock("../../hooks/useServerWorkspaces", () => ({
  useServerWorkspaces: () => ({ data: [{ id: "one" }, { id: "two" }], isLoading: false })
}));

describe("WorkspaceSwitcher", () => {
  it("summarizes server workspaces and opens their management page", () => {
    const onManage = vi.fn();
    render(<ThemeProvider theme={darkTheme}><WorkspaceSwitcher collapsed={false} onManage={onManage} /></ThemeProvider>);

    expect(screen.getByText("2 registered")).toBeInTheDocument();
    expect(screen.getByText("Selection is saved per conversation")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Manage workspaces" }));
    expect(onManage).toHaveBeenCalledOnce();
  });
});
