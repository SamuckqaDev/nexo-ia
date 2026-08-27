import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { describe, expect, it, vi } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import type { ServerWorkspace } from "../../types/serverWorkspaceTypes";
import { WorkspacePickerControl } from ".";

const workspace: ServerWorkspace = {
  id: "427d6713-f2d4-4b0d-8f72-eaa7f19ebd23",
  name: "nexo-ia",
  storageType: "UNBOUND",
  accessMode: "READ_ONLY",
  status: "AVAILABLE",
  relativePath: null,
  lastScannedAt: null,
  createdAt: "2026-08-27T00:00:00Z",
  updatedAt: "2026-08-27T00:00:00Z"
};

describe("WorkspacePickerControl", () => {
  it("selects existing workspaces and exposes the native folder action", () => {
    const onSelect = vi.fn();
    const onChooseLocal = vi.fn();
    render(
      <ThemeProvider theme={darkTheme}>
        <WorkspacePickerControl
          workspaceId={null}
          workspaces={[workspace]}
          selectDisabled={false}
          localDisabled={false}
          localAvailable
          localPending={false}
          onSelect={onSelect}
          onChooseLocal={onChooseLocal}
        />
      </ThemeProvider>
    );

    fireEvent.change(screen.getByRole("combobox", { name: "Conversation workspace" }), {
      target: { value: workspace.id }
    });
    fireEvent.click(screen.getByRole("button", { name: "Choose a project folder from this computer" }));

    expect(onSelect).toHaveBeenCalledWith(workspace.id);
    expect(onChooseLocal).toHaveBeenCalledOnce();
  });

  it("keeps the folder action explicit when the desktop bridge is unavailable", () => {
    const onChooseLocal = vi.fn();
    render(
      <ThemeProvider theme={darkTheme}>
        <WorkspacePickerControl
          workspaceId={null}
          workspaces={[]}
          selectDisabled={false}
          localDisabled={false}
          localAvailable={false}
          localPending={false}
          onSelect={vi.fn()}
          onChooseLocal={onChooseLocal}
        />
      </ThemeProvider>
    );

    const button = screen.getByRole("button", { name: "Choose a project folder from this computer" });
    expect(button).toHaveAttribute("title", "Native folder picker unavailable. Restart Nexo Desktop");

    fireEvent.click(button);
    expect(onChooseLocal).toHaveBeenCalledOnce();
  });
});
