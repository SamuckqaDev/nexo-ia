import { fireEvent, render, screen } from "@testing-library/react";
import { FolderOpen } from "@phosphor-icons/react";
import { describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "styled-components";
import { darkTheme } from "../../styles/theme";
import { PlaceholderPage } from "./index";

describe("Planned workspace surface", () => {
  it("shows the documented release and never implies that execution is available", () => {
    const onStartInChat = vi.fn();

    render(
      <ThemeProvider theme={darkTheme}>
        <PlaceholderPage
          title="Projects"
          eyebrow="Governed workspaces"
          description="Authorize an exact project directory."
          release="Release 0.3"
          icon={FolderOpen}
          capabilities={[{ title: "Scoped access", description: "Use an explicit directory." }]}
          onStartInChat={onStartInChat}
        />
      </ThemeProvider>
    );

    expect(screen.getByRole("heading", { name: "Projects" })).toBeInTheDocument();
    expect(screen.getByText("Release 0.3")).toBeInTheDocument();
    expect(screen.getByText(/execution runtime is not enabled yet/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /start with chat/i }));
    expect(onStartInChat).toHaveBeenCalledOnce();
  });
});
