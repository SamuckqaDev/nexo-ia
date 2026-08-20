import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "styled-components";
import { darkTheme } from "../../../../../app/styles/theme";
import type { Conversation } from "../../types/chatTypes";
import { ConversationSidebar } from "./index";

const conversation: Conversation = {
  id: "c98c49c9-0ffe-44b1-b597-c88326e14a7a",
  title: "Responsive Nexo",
  providerConfigurationId: null,
  selectedModel: null,
  createdAt: "2026-08-20T12:00:00Z",
  updatedAt: "2026-08-20T12:00:00Z"
};

const renderSidebar = (open: boolean, overrides: Record<string, unknown> = {}) => {
  const props = {
    conversations: [conversation],
    selectedId: conversation.id,
    isCreating: false,
    open,
    onSelect: vi.fn(),
    onNew: vi.fn(),
    onArchive: vi.fn(),
    onOpenChange: vi.fn(),
    ...overrides
  };
  render(
    <ThemeProvider theme={darkTheme}>
      <ConversationSidebar {...props} />
    </ThemeProvider>
  );
  return props;
};

afterEach(() => vi.unstubAllGlobals());

describe("ConversationSidebar", () => {
  it("minimizes into a rail without changing the selected conversation", () => {
    const props = renderSidebar(true);

    fireEvent.click(screen.getByRole("button", { name: "Minimize conversation menu" }));

    expect(props.onOpenChange).toHaveBeenCalledWith(false);
    expect(props.onSelect).not.toHaveBeenCalled();
    expect(props.onArchive).not.toHaveBeenCalled();
  });

  it("expands and can create a conversation from the compact rail", () => {
    const props = renderSidebar(false);

    fireEvent.click(screen.getByRole("button", { name: "Expand conversation menu" }));
    fireEvent.click(screen.getByRole("button", { name: "New conversation" }));

    expect(props.onOpenChange).toHaveBeenCalledWith(true);
    expect(props.onNew).toHaveBeenCalledOnce();
  });

  it("selects a conversation and closes the drawer on compact viewports", () => {
    vi.stubGlobal("matchMedia", vi.fn().mockReturnValue({ matches: true }));
    const props = renderSidebar(true);

    fireEvent.click(screen.getByRole("button", { name: "Open Responsive Nexo" }));

    expect(props.onSelect).toHaveBeenCalledWith(conversation.id);
    expect(props.onOpenChange).toHaveBeenCalledWith(false);
  });

  it("preserves archive behavior while expanded", () => {
    const props = renderSidebar(true);

    fireEvent.click(screen.getByRole("button", { name: "Archive Responsive Nexo" }));

    expect(props.onArchive).toHaveBeenCalledWith(conversation);
  });
});
