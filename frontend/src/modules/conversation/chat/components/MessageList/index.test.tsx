import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ThemeProvider } from "styled-components";
import { darkTheme } from "../../../../../app/styles/theme";
import { MessageList } from "./index";

describe("MessageList", () => {
  afterEach((): void => {
    vi.useRealTimers();
  });

  it("uses the Nexo loading identity while conversation history is restored", () => {
    render(
      <ThemeProvider theme={darkTheme}>
        <MessageList
          messages={[]}
          isLoading
          hasConversation
          hasModel
          hasConfiguredProvider
          phase="idle"
          startedAt={null}
          thinkingContent=""
          streamingContent=""
          errorMessage={null}
          mode="chat"
          onConfigureProvider={(): void => undefined}
        />
      </ThemeProvider>
    );

    expect(screen.getByRole("status")).toHaveTextContent("Opening conversation");
  });

  it("does not fabricate a Thinking card before the first model token", () => {
    render(
      <ThemeProvider theme={darkTheme}>
        <MessageList
          messages={[]}
          isLoading={false}
          hasConversation
          hasModel
          hasConfiguredProvider
          phase="starting"
          startedAt={Date.now()}
          thinkingContent=""
          streamingContent=""
          errorMessage={null}
          mode="chat"
          onConfigureProvider={(): void => undefined}
        />
      </ThemeProvider>
    );

    expect(screen.getByText(/waiting for model output/i)).toBeInTheDocument();
    expect(screen.getByText(/you can open another chat and come back/i)).toBeVisible();
    expect(screen.queryByText("Thinking")).not.toBeInTheDocument();
    expect(screen.queryByText(/preparing the selected model/i)).not.toBeInTheDocument();
  });

  it("offers a path to provider setup when no model is configured yet", () => {
    const onConfigureProvider = vi.fn();
    render(
      <ThemeProvider theme={darkTheme}>
        <MessageList
          messages={[]}
          isLoading={false}
          hasConversation
          hasModel={false}
          hasConfiguredProvider={false}
          phase="idle"
          startedAt={null}
          thinkingContent=""
          streamingContent=""
          errorMessage={null}
          mode="chat"
          onConfigureProvider={onConfigureProvider}
        />
      </ThemeProvider>
    );

    fireEvent.click(screen.getByRole("button", { name: /configure a provider/i }));

    expect(onConfigureProvider).toHaveBeenCalledOnce();
  });

  it("points at the header picker when a provider is configured but the conversation has no model", () => {
    render(
      <ThemeProvider theme={darkTheme}>
        <MessageList
          messages={[]}
          isLoading={false}
          hasConversation
          hasModel={false}
          hasConfiguredProvider
          phase="idle"
          startedAt={null}
          thinkingContent=""
          streamingContent=""
          errorMessage={null}
          mode="chat"
          onConfigureProvider={(): void => undefined}
        />
      </ThemeProvider>
    );

    expect(screen.getByText(/pick one of your configured local models/i)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /configure a provider/i })).not.toBeInTheDocument();
  });

  it("shows only real streamed Thinking and labels it as temporary", () => {
    render(
      <ThemeProvider theme={darkTheme}>
        <MessageList
          messages={[{
            id: "0c29a88f-ed8d-42f6-abbb-a373d096fd9a",
            role: "ASSISTANT",
            status: "STREAMING",
            content: "",
            model: "qwen3:8b",
            inputTokens: null,
            outputTokens: null,
            totalTokens: null,
            contextTokensUsed: null,
            contextTokenBudget: 8000,
            tokenSource: null,
            latencyMs: null,
            processingLocation: "LOCAL",
            failureCode: null,
            createdAt: "2026-08-20T12:00:00Z",
            completedAt: null
          }]}
          isLoading={false}
          hasConversation
          hasModel
          hasConfiguredProvider
          phase="streaming"
          startedAt={Date.now()}
          thinkingContent="Checking the available evidence."
          streamingContent=""
          errorMessage={null}
          mode="chat"
          onConfigureProvider={(): void => undefined}
        />
      </ThemeProvider>
    );

    expect(screen.getByText("Checking the available evidence.")).toBeInTheDocument();
    expect(screen.getByText(/not saved/i)).toBeInTheDocument();
  });

  it("keeps a visible loading indicator and elapsed timer while response tokens arrive", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-08-20T19:00:00Z"));

    render(
      <ThemeProvider theme={darkTheme}>
        <MessageList
          messages={[]}
          isLoading={false}
          hasConversation
          hasModel
          hasConfiguredProvider
          phase="streaming"
          startedAt={Date.now()}
          thinkingContent=""
          streamingContent="Partial answer"
          errorMessage={null}
          mode="chat"
          onConfigureProvider={(): void => undefined}
        />
      </ThemeProvider>
    );

    expect(screen.getByText("Generating response")).toBeVisible();
    expect(screen.getByRole("timer", { name: /response generation elapsed time/i })).toHaveTextContent("00:00");

    act((): void => {
      vi.advanceTimersByTime(3_000);
    });

    expect(screen.getByRole("timer", { name: /response generation elapsed time/i })).toHaveTextContent("00:03");
  });
});
