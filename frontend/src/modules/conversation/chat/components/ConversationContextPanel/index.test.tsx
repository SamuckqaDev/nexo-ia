import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "styled-components";
import { darkTheme } from "../../../../../app/styles/theme";
import { useImageGenerationStore } from "../../../media/stores/useImageGenerationStore";
import { ConversationContextPanel } from "./index";

const renderPanel = (mode: "chat" | "agent", open = true, onOpenChange = vi.fn()) => render(
  <ThemeProvider theme={darkTheme}>
    <ConversationContextPanel conversationId="conversation-1" mode={mode} open={open} onOpenChange={onOpenChange} onManageVaults={vi.fn()} onManageWorkspace={vi.fn()} />
  </ThemeProvider>
);

describe("ConversationContextPanel", () => {
  beforeEach(() => useImageGenerationStore.getState().reset());

  it("keeps the chat plan honest until Agent mode is selected", () => {
    renderPanel("chat");

    fireEvent.click(screen.getByRole("tab", { name: /plan/i }));
    expect(screen.getByText("No active plan")).toBeInTheDocument();
    expect(screen.getByText(/switch to agent/i)).toBeInTheDocument();
  });

  it("shows the governed plan preview without claiming that the runtime is active", () => {
    renderPanel("agent");

    fireEvent.click(screen.getByRole("tab", { name: /plan/i }));
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

  it("shows reported image progress, elapsed time and remaining estimate", () => {
    useImageGenerationStore.getState().upsertJob({
      id: "image-1",
      conversationId: "conversation-1",
      prompt: "A cyan architecture diagram",
      status: "GENERATING",
      progress: 42,
      etaSeconds: 18,
      startedAt: new Date().toISOString(),
      errorMessage: null
    });
    renderPanel("chat");

    fireEvent.click(screen.getByRole("tab", { name: /media/i }));

    expect(screen.getByRole("progressbar", { name: /image generation progress/i })).toHaveValue(42);
    expect(screen.getByText("42%")).toBeVisible();
    expect(screen.getByText(/about 18s remaining/i)).toBeVisible();
  });
});
