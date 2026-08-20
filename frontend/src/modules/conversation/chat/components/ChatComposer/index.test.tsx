import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "styled-components";
import { darkTheme } from "../../../../../app/styles/theme";
import { previewVaults, useVaultCatalogStore } from "../../../../knowledge/vault/stores/useVaultCatalogStore";
import { builtInSkills, useSkillCatalogStore } from "../../../../skill/catalog/stores/useSkillCatalogStore";
import { ChatComposer } from "./index";

const renderComposer = (
  mode: "chat" | "agent",
  onModeChange = vi.fn(),
  initialContent = "",
  onSend = vi.fn()
) => {
  render(
    <ThemeProvider theme={darkTheme}>
      <ChatComposer
        disabled={false}
        initialContent={initialContent}
        hasModel
        phase="idle"
        isBusy={false}
        mode={mode}
        onModeChange={onModeChange}
        onSend={onSend}
        onCancel={vi.fn()}
      />
    </ThemeProvider>
  );
  return { onSend };
};

describe("ChatComposer", () => {
  beforeEach(() => {
    useSkillCatalogStore.setState({ skills: builtInSkills });
    useVaultCatalogStore.setState({ vaults: previewVaults, attachedSourceIds: [] });
  });

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

  it("opens the Skill palette with slash and includes the chosen method in the message context", () => {
    const onSend = vi.fn();
    renderComposer("chat", vi.fn(), "", onSend);
    const message = screen.getByRole("textbox", { name: "Message" });

    fireEvent.change(message, { target: { value: "/pro" } });
    fireEvent.click(screen.getByRole("option", { name: /Project review/i }));

    expect(screen.getByText("Skill: /project-review")).toBeInTheDocument();
    fireEvent.change(message, { target: { value: "Review this module" } });
    fireEvent.keyDown(message, { key: "Enter" });

    expect(onSend).toHaveBeenCalledOnce();
    expect(onSend.mock.calls[0][0]).toContain("NEXO_EXPLICIT_CONTEXT");
    expect(onSend.mock.calls[0][0]).toContain("Review this module");
    expect(onSend.mock.calls[0][0]).toContain("verify each finding");
  });

  it("keeps the Skill palette visible above the conversation", () => {
    renderComposer("chat");
    const message = screen.getByRole("textbox", { name: "Message" });

    fireEvent.change(message, { target: { value: "/" } });

    expect(screen.getByRole("listbox", { name: "Available Skills" })).toBeVisible();
    expect(message.closest("form")?.parentElement).toHaveStyle({ overflow: "hidden" });
    expect(message.closest("form")?.parentElement?.parentElement).toHaveStyle({
      overflow: "visible",
      position: "relative",
      zIndex: "1"
    });
  });
});
