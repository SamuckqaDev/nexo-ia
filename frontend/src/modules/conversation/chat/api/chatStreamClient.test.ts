import { afterEach, describe, expect, it, vi } from "vitest";
import { streamMessage } from "./chatStreamClient";
import type { ChatStreamHandlers } from "../types/chatTypes";

const handlers: ChatStreamHandlers = {
  onStarted: vi.fn(),
  onThinking: vi.fn(),
  onToken: vi.fn(),
  onUsage: vi.fn(),
  onCompleted: vi.fn(),
  onCancelled: vi.fn(),
  onError: vi.fn()
};

afterEach((): void => {
  vi.restoreAllMocks();
});

describe("streamMessage", () => {
  it.each([false, true])("sends the Thinking preference as %s", async (thinkingEnabled: boolean) => {
    const fetchMock = vi.spyOn(globalThis, "fetch")
      .mockResolvedValue(new Response("", { status: 200 }));

    await streamMessage(
      "23ab2ec1-9fc5-4dd9-a18c-6cc8b62130c5",
      "hello",
      thinkingEnabled,
      handlers,
      new AbortController().signal
    );

    const request = fetchMock.mock.calls[0]?.[1];
    expect(JSON.parse(String(request?.body))).toEqual({ content: "hello", thinkingEnabled });
  });
});
