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
  it("shows root entries and expands saved folders on demand", () => {
    render(<ThemeProvider theme={darkTheme}><WorkspaceTree snapshot={snapshot} /></ThemeProvider>);

    expect(screen.getByText("1 folders · 2 files")).toBeInTheDocument();
    expect(screen.getByText("README.md")).toBeInTheDocument();
    expect(screen.queryByText("app.tsx")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /src/i }));
    expect(screen.getByText("app.tsx")).toBeInTheDocument();
  });
});
