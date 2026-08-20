import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { describe, expect, it, vi } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import { WorkspaceChangeNotice } from ".";

describe("WorkspaceChangeNotice", () => {
  it("warns the user when the project structure changed", () => {
    const accept = vi.fn();
    render(
      <ThemeProvider theme={darkTheme}>
        <WorkspaceChangeNotice
          workspaceName="Nexo IA"
          check={{
            workspaceId: "workspace-id",
            status: "changed",
            checkedAt: "2026-08-20T00:00:00Z",
            message: "changed",
            changes: { added: 2, removed: 1, modified: 3, samples: ["src/App.tsx"], truncated: false }
          }}
          onManage={vi.fn()}
          onRecheck={vi.fn()}
          onAccept={accept}
        />
      </ThemeProvider>
    );

    expect(screen.getByRole("alert")).toHaveTextContent("Nexo IA changed");
    expect(screen.getByText(/2 additions/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Use updated structure" }));
    expect(accept).toHaveBeenCalledOnce();
  });
});
