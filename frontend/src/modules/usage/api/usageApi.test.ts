import { describe, expect, it } from "vitest";
import { usageSummarySchema } from "../schemas/usageSchemas";

describe("usageSummarySchema", () => {
  const base = {
    period: "LAST_7_DAYS",
    from: "2026-08-11T00:00:00Z",
    to: "2026-08-18T00:00:00Z",
    totals: {
      requests: 3, completed: 2, cancelled: 1, failed: 0,
      inputTokens: 50, outputTokens: 10, totalTokens: 60,
      averageLatencyMs: 1500, estimatedTokenRequests: 0
    },
    daily: [{ date: "2026-08-18", requests: 3, inputTokens: 50, outputTokens: 10 }],
    byModel: [{ model: "qwen3:8b", requests: 3, inputTokens: 50, outputTokens: 10, averageLatencyMs: 1500 }],
    byProcessingLocation: [{ processingLocation: "LOCAL", requests: 3, totalTokens: 60 }]
  };

  it("accepts a well-formed summary", () => {
    expect(usageSummarySchema.safeParse(base).success).toBe(true);
  });

  it("accepts a null average latency for an empty window", () => {
    const empty = { ...base, totals: { ...base.totals, averageLatencyMs: null } };
    expect(usageSummarySchema.safeParse(empty).success).toBe(true);
  });

  it("rejects an unknown processing location", () => {
    const invalid = { ...base, byProcessingLocation: [{ processingLocation: "CLOUD", requests: 1, totalTokens: 5 }] };
    expect(usageSummarySchema.safeParse(invalid).success).toBe(false);
  });

  it("rejects an unknown period", () => {
    expect(usageSummarySchema.safeParse({ ...base, period: "LAST_YEAR" }).success).toBe(false);
  });
});
