import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "styled-components";
import { darkTheme } from "../../../../../../../app/styles/theme";
import { builtInSkills } from "../../../../../../skill/catalog/stores/useSkillCatalogStore";
import { buildContextualChatMessage } from "../../../../services/chatContextService";
import type { ConversationMessage } from "../../../../types/chatTypes";
import { MessageItem } from "./index";

const message = (overrides: Partial<ConversationMessage> = {}): ConversationMessage => ({
  id: "11111111-1111-1111-1111-111111111111",
  role: "ASSISTANT",
  status: "COMPLETED",
  content: "Hello",
  model: "qwen3:8b",
  inputTokens: 20,
  outputTokens: 3,
  totalTokens: 23,
  contextTokensUsed: 20,
  contextTokenBudget: 8000,
  tokenSource: "PROVIDER",
  latencyMs: 1500,
  processingLocation: "LOCAL",
  failureCode: null,
  createdAt: "2026-08-18T12:00:00Z",
  completedAt: "2026-08-18T12:00:02Z",
  ...overrides
});

const renderItem = (value: ConversationMessage) =>
  render(<ThemeProvider theme={darkTheme}><MessageItem message={value} /></ThemeProvider>);

describe("MessageItem", () => {
  it("shows the model, token usage and latency of a completed answer", () => {
    renderItem(message());

    expect(screen.getByText("qwen3:8b")).toBeInTheDocument();
    expect(screen.getByText("20 in · 3 out")).toBeInTheDocument();
    expect(screen.getByText("23")).toBeInTheDocument();
    expect(screen.getByText(/Context 20 \/ 8,000 \(0.3%\)/)).toBeInTheDocument();
    expect(screen.getByText("Response time 1.5s")).toBeInTheDocument();
  });

  it("labels an estimated token count instead of presenting it as measured", () => {
    renderItem(message({ tokenSource: "ESTIMATE" }));

    expect(screen.getByText("20 in · 3 out (estimated)")).toBeInTheDocument();
  });

  it("states that a cancelled answer was stopped rather than showing it as normal", () => {
    renderItem(message({ status: "CANCELLED", content: "Hel" }));

    expect(screen.getByText(/you stopped this answer/i)).toBeInTheDocument();
    expect(screen.getByText(/Hel/)).toBeInTheDocument();
  });

  it("states that a failed answer did not complete", () => {
    renderItem(message({ status: "FAILED", content: "", failureCode: "PROVIDER_STREAM_FAILED" }));

    expect(screen.getByText(/failed and was not completed/i)).toBeInTheDocument();
  });

  it("keeps live thinking collapsed inside the Nexo answer bubble", () => {
    render(
      <ThemeProvider theme={darkTheme}>
        <MessageItem message={message({ content: "", status: "STREAMING" })} isStreaming thinkingContent="Checking the request" />
      </ThemeProvider>
    );

    expect(screen.getByText("Thinking")).toBeInTheDocument();
    expect(screen.getByText("Checking the request")).toBeInTheDocument();
    expect(screen.getByText("Thinking").closest("details")).not.toHaveAttribute("open");
  });

  it("does not show execution metadata on a user message", () => {
    renderItem(message({ role: "USER", content: "hi" }));

    expect(screen.queryByText("qwen3:8b")).not.toBeInTheDocument();
  });

  it("keeps a visible Skill marker on the sent message", () => {
    renderItem(message({
      role: "USER",
      content: buildContextualChatMessage("Review this module", { skill: builtInSkills[0], vaultSources: [] })
    }));

    expect(screen.getByText("Review this module")).toBeVisible();
    expect(screen.getByText("Skill: Project review")).toBeVisible();
  });

  it("copies the visible message content", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", { configurable: true, value: { writeText } });
    renderItem(message({ content: "Copy this answer" }));

    fireEvent.click(screen.getByRole("button", { name: "Copy message" }));

    await waitFor(() => expect(writeText).toHaveBeenCalledWith("Copy this answer"));
    expect(screen.getByRole("button", { name: "Content copied" })).toBeInTheDocument();
  });
});
