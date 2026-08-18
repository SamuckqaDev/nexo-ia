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
      data: { inputTokens: 20, outputTokens: 3, tokenSource: "ESTIMATE", latencyMs: 1200 }
    });

    expect(parsed.success).toBe(true);
  });

  it("accepts a usage frame without provider counts", () => {
    const parsed = streamEventSchema.safeParse({
      event: "usage",
      data: { inputTokens: null, outputTokens: null, tokenSource: null, latencyMs: 900 }
    });

    expect(parsed.success).toBe(true);
  });

  it("rejects an unknown event name", () => {
    const parsed = streamEventSchema.safeParse({
      event: "thinking",
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
