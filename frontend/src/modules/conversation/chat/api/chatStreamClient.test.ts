import { afterEach, describe, expect, it, vi } from "vitest";
import { streamMessage } from "./chatStreamClient";
import type { ChatStreamHandlers } from "../types/chatTypes";

const { refreshAuthenticatedSessionMock } = vi.hoisted(() => ({
  refreshAuthenticatedSessionMock: vi.fn()
}));

vi.mock("../../../../shared/api/client", () => ({
  refreshAuthenticatedSession: refreshAuthenticatedSessionMock
}));

const handlers: ChatStreamHandlers = {
  onStarted: vi.fn(),
  onThinking: vi.fn(),
  onAgentState: vi.fn(),
  onToolStarted: vi.fn(),
      onToolCompleted: vi.fn(),
      onPlanUpdated: vi.fn(),
  onToken: vi.fn(),
  onUsage: vi.fn(),
  onCompleted: vi.fn(),
  onCancelled: vi.fn(),
  onError: vi.fn()
};

afterEach((): void => {
  vi.restoreAllMocks();
  vi.clearAllMocks();
});

describe("streamMessage", () => {
  it.each([false, true])("sends the Thinking preference as %s", async (thinkingEnabled: boolean) => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValue(new Response("", { status: 200 }));

    await streamMessage(
      "23ab2ec1-9fc5-4dd9-a18c-6cc8b62130c5",
      "hello",
      thinkingEnabled,
      "chat",
      handlers,
      new AbortController().signal
    );

    const request = fetchMock.mock.calls[0]?.[1];
    expect(JSON.parse(String(request?.body)))
      .toEqual({ content: "hello", thinkingEnabled, mode: "chat" });
  });

  it("dispatches the final SSE frame even when the connection closes without a blank separator", async () => {
    const messageId = "23ab2ec1-9fc5-4dd9-a18c-6cc8b62130c5";
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(
      `event: completed\ndata: {"messageId":"${messageId}","content":"Ready","completedAt":"2026-08-20T18:44:02Z"}`,
      { status: 200, headers: { "Content-Type": "text/event-stream" } }
    ));

    await streamMessage(
      "33ab2ec1-9fc5-4dd9-a18c-6cc8b62130c6",
      "hello",
      false,
      "chat",
      handlers,
      new AbortController().signal
    );

    expect(handlers.onCompleted).toHaveBeenCalledWith(expect.objectContaining({
      messageId,
      content: "Ready"
    }));
  });

  it("refreshes an expired session once before retrying a rejected stream", async () => {
    const messageId = "23ab2ec1-9fc5-4dd9-a18c-6cc8b62130c5";
    refreshAuthenticatedSessionMock.mockResolvedValue(undefined);
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify({
        code: 401,
        message: "An authenticated session is required",
        data: []
      }), { status: 401, headers: { "Content-Type": "application/json" } }))
      .mockResolvedValueOnce(new Response(
        `event: completed\ndata: {"messageId":"${messageId}","content":"Olá","completedAt":"2026-08-20T22:00:51Z"}`,
        { status: 200, headers: { "Content-Type": "text/event-stream" } }
      ));

    await streamMessage(
      "33ab2ec1-9fc5-4dd9-a18c-6cc8b62130c6",
      "oi",
      false,
      "chat",
      handlers,
      new AbortController().signal
    );

    expect(refreshAuthenticatedSessionMock).toHaveBeenCalledOnce();
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(handlers.onCompleted).toHaveBeenCalledWith(expect.objectContaining({ content: "Olá" }));
  });
});
