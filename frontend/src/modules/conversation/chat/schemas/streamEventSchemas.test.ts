import { describe, expect, it } from "vitest";
import { streamEventSchema } from "./streamEventSchemas";

describe("streamEventSchema", () => {
  it("accepts a token frame", () => {
    const parsed = streamEventSchema.safeParse({
      event: "token",
      data: { content: "Hel", index: 0 }
    });

    expect(parsed.success).toBe(true);
  });

  it("accepts a usage frame reporting an estimate", () => {
    const parsed = streamEventSchema.safeParse({
      event: "usage",
      data: {
        inputTokens: 20,
        outputTokens: 3,
        totalTokens: 23,
        contextTokensUsed: 20,
        contextTokenBudget: 8000,
        tokenSource: "ESTIMATE",
        latencyMs: 1200
      }
    });

    expect(parsed.success).toBe(true);
  });

  it("accepts a usage frame without provider counts", () => {
    const parsed = streamEventSchema.safeParse({
      event: "usage",
      data: {
        inputTokens: null,
        outputTokens: null,
        totalTokens: null,
        contextTokensUsed: null,
        contextTokenBudget: 8000,
        tokenSource: null,
        latencyMs: 900
      }
    });

    expect(parsed.success).toBe(true);
  });

  it("accepts a real Thinking frame", () => {
    const parsed = streamEventSchema.safeParse({
      event: "thinking",
      data: { content: "Checking the evidence", index: 0 }
    });

    expect(parsed.success).toBe(true);
  });

  it("rejects an unknown event name", () => {
    const parsed = streamEventSchema.safeParse({
      event: "provider_progress",
      data: { content: "..." }
    });

    expect(parsed.success).toBe(false);
  });

  it("rejects a completed frame without a message identifier", () => {
    const parsed = streamEventSchema.safeParse({
      event: "completed",
      data: { content: "Hello", completedAt: "2026-08-18T12:00:00Z" }
    });

    expect(parsed.success).toBe(false);
  });

  it("rejects a token frame whose index is not a number", () => {
    const parsed = streamEventSchema.safeParse({
      event: "token",
      data: { content: "Hel", index: "0" }
    });

    expect(parsed.success).toBe(false);
  });
});
