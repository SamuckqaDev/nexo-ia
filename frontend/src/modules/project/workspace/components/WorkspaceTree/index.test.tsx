import { fireEvent, render, screen } from "@testing-library/react";
import { ThemeProvider } from "styled-components";
import { describe, expect, it } from "vitest";
import { darkTheme } from "../../../../../app/styles/theme";
import type { WorkspaceSnapshot } from "../../types/workspaceSnapshotTypes";
import { WorkspaceTree } from "./index";

const snapshot: WorkspaceSnapshot = {
  capturedAt: "2026-08-20T12:00:00Z",
  truncated: false,
  entries: [
    { path: "src", kind: "directory", size: null, lastModified: null },
    { path: "src/app.tsx", kind: "file", size: 2048, lastModified: 1 },
    { path: "README.md", kind: "file", size: 1024, lastModified: 1 }
  ]
};

describe("WorkspaceTree", () => {
  it("shows the first project level immediately and lets the user collapse it", () => {
    render(<ThemeProvider theme={darkTheme}><WorkspaceTree snapshot={snapshot} /></ThemeProvider>);

    expect(screen.getByText("1 folders · 2 files")).toBeInTheDocument();
    expect(screen.getByText("README.md")).toBeInTheDocument();
    expect(screen.getByText("app.tsx")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /src/i }));
    expect(screen.queryByText("app.tsx")).not.toBeInTheDocument();
  });

  it("filters the complete captured tree by path", () => {
    render(<ThemeProvider theme={darkTheme}><WorkspaceTree snapshot={snapshot} /></ThemeProvider>);

    fireEvent.change(screen.getByRole("searchbox", { name: /filter workspace structure/i }), {
      target: { value: "src/app" }
    });

    expect(screen.getByText("src")).toBeInTheDocument();
    expect(screen.getByText("app.tsx")).toBeInTheDocument();
    expect(screen.queryByText("README.md")).not.toBeInTheDocument();
  });

  it("explains which paths were not scanned and why", () => {
    const partialSnapshot: WorkspaceSnapshot = {
      ...snapshot,
      truncated: true,
      entries: [...snapshot.entries, { path: "node_modules", kind: "directory", size: null, lastModified: null }],
      scan: {
        maxEntries: 20_000,
        maxDepth: 32,
        omissionCount: 1,
        omissions: [{ path: "node_modules", reason: "ignored-directory" }]
      }
    };

    render(<ThemeProvider theme={darkTheme}><WorkspaceTree snapshot={partialSnapshot} /></ThemeProvider>);

    expect(screen.getByText("Partial snapshot")).toBeInTheDocument();
    expect(screen.getByText("not scanned")).toBeInTheDocument();
    fireEvent.click(screen.getByText(/1 path not fully scanned/i));
    expect(screen.getByText("Generated or dependency directory skipped")).toBeInTheDocument();
  });
});
