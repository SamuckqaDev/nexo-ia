import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "styled-components";
import { darkTheme } from "../../../../../app/styles/theme";
import { ConversationContextPanel } from "./index";

const renderPanel = (mode: "chat" | "agent", open = true, onOpenChange = vi.fn()) => render(
  <ThemeProvider theme={darkTheme}>
    <ConversationContextPanel mode={mode} open={open} onOpenChange={onOpenChange} />
  </ThemeProvider>
);

describe("ConversationContextPanel", () => {
  it("keeps the chat plan honest until Agent mode is selected", () => {
    renderPanel("chat");

    expect(screen.getByText("No active plan")).toBeInTheDocument();
    expect(screen.getByText(/switch to agent/i)).toBeInTheDocument();
  });

  it("shows the governed plan preview without claiming that the runtime is active", () => {
    renderPanel("agent");

    expect(screen.getByText("Runtime preview")).toBeInTheDocument();
    expect(screen.getByText("Execute and verify")).toBeInTheDocument();
    expect(screen.getByText(/when the governed Agent runtime is connected/i)).toBeInTheDocument();
  });

  it("exposes tasks, artifacts and media as conversation resources", () => {
    renderPanel("chat");

    fireEvent.click(screen.getByRole("tab", { name: /tasks/i }));
    expect(screen.getByText("No tasks yet")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("tab", { name: /artifacts/i }));
    expect(screen.getByText("No artifacts yet")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("tab", { name: /media/i }));
    expect(screen.getByText("No media yet")).toBeInTheDocument();
  });

  it("keeps a navigable resource rail when the workspace is minimized", () => {
    const onOpenChange = vi.fn();
    renderPanel("chat", false, onOpenChange);

    fireEvent.click(screen.getByRole("button", { name: "Media" }));

    expect(onOpenChange).toHaveBeenCalledWith(true);
  });
});
