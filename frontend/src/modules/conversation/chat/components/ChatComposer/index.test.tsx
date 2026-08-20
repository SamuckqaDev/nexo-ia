import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "styled-components";
import { darkTheme } from "../../../../../app/styles/theme";
import { ChatComposer } from "./index";

const renderComposer = (
  mode: "chat" | "agent",
  onModeChange = vi.fn(),
  initialContent = ""
) => render(
  <ThemeProvider theme={darkTheme}>
    <ChatComposer
      disabled={false}
      initialContent={initialContent}
      hasModel
      phase="idle"
      isBusy={false}
      mode={mode}
      onModeChange={onModeChange}
      onSend={vi.fn()}
      onCancel={vi.fn()}
    />
  </ThemeProvider>
);

describe("ChatComposer", () => {
  it("keeps Chat as the active conversational surface and exposes capabilities in the composer", () => {
    renderComposer("chat");

    expect(screen.getByRole("textbox", { name: "Message" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Tools" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Image" })).toBeDisabled();
  });

  it("requests Agent mode without replacing the conversation surface", () => {
    const onModeChange = vi.fn();
    renderComposer("chat", onModeChange);

    fireEvent.click(screen.getByRole("button", { name: "Agent" }));

    expect(onModeChange).toHaveBeenCalledWith("agent");
  });

  it("opens with a request drafted from the workspace Home", () => {
    renderComposer("chat", vi.fn(), "Review the authentication flow");

    expect(screen.getByRole("textbox", { name: "Message" }))
      .toHaveValue("Review the authentication flow");
  });

  it("explains the unavailable Agent runtime inside the composer", () => {
    renderComposer("agent");

    expect(screen.getByText(/adds a visible plan, permissions and verified steps/i)).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "Message" })).toBeDisabled();
  });
});
