import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import { ApiError } from "../../../../shared/api/ApiError";
import type { ChatStreamHandlers } from "../types/chatTypes";
import { resetChatStreams, useChatStream } from "./useChatStream";

const { streamMessageMock } = vi.hoisted(() => ({ streamMessageMock: vi.fn() }));

vi.mock("../api/chatStreamClient", () => ({ streamMessage: streamMessageMock }));
vi.mock("../api/chatApi", () => ({ cancelModelRequest: vi.fn().mockResolvedValue(undefined) }));

const firstConversation = "10000000-0000-4000-8000-000000000001";
const secondConversation = "20000000-0000-4000-8000-000000000002";

describe("useChatStream", () => {
  afterEach(() => {
    resetChatStreams();
    vi.clearAllMocks();
  });

  it("keeps a request alive and restores its local progress after another chat is opened", () => {
    let handlers: ChatStreamHandlers | null = null;
    let signal: AbortSignal | null = null;
    const requestSignal = (): AbortSignal => {
      if (!signal) throw new Error("The stream did not expose its cancellation signal");
      return signal;
    };
    streamMessageMock.mockImplementation((
      _conversationId: string,
      _content: string,
      _thinkingEnabled: boolean,
      _knowledgeVaultIds: string[],
      nextHandlers: ChatStreamHandlers,
      nextSignal: AbortSignal
    ): Promise<void> => {
      handlers = nextHandlers;
      signal = nextSignal;
      return new Promise<void>(() => undefined);
    });
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }): ReactNode => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    const { result, rerender } = renderHook(
      ({ conversationId }: { conversationId: string }) => useChatStream(conversationId, []),
      { initialProps: { conversationId: firstConversation }, wrapper }
    );

    act((): void => result.current.send("Review this project"));
    const startedAt: number | null = result.current.startedAt;
    act((): void => {
      handlers?.onStarted({
        userMessageId: "30000000-0000-4000-8000-000000000003",
        assistantMessageId: "40000000-0000-4000-8000-000000000004",
        correlationId: "50000000-0000-4000-8000-000000000005",
        model: "qwen3:8b",
        processingLocation: "LOCAL"
      });
      handlers?.onToken({ content: "Working", index: 0 });
    });

    rerender({ conversationId: secondConversation });
    expect(result.current.phase).toBe("idle");
    expect(requestSignal().aborted).toBe(false);

    rerender({ conversationId: firstConversation });
    expect(result.current.phase).toBe("streaming");
    expect(result.current.startedAt).toBe(startedAt);
    expect(result.current.streamingContent).toBe("Working");
    expect(requestSignal().aborted).toBe(false);
  });

  it("leaves session expiry to re-authentication instead of printing it inside the chat", async () => {
    streamMessageMock.mockRejectedValue(new ApiError(
      401, "An authenticated session is required", 401));
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const wrapper = ({ children }: { children: ReactNode }): ReactNode => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    const { result } = renderHook(
      () => useChatStream(firstConversation, []),
      { wrapper }
    );

    act((): void => result.current.send("Keep this out of the message list"));

    await waitFor(() => expect(result.current.phase).toBe("failed"));
    expect(result.current.errorMessage).toBeNull();
  });
});
