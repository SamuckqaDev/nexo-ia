import { z } from "zod";

export const usagePeriodSchema = z.enum([
  "LAST_24_HOURS",
  "LAST_7_DAYS",
  "LAST_30_DAYS",
  "ALL_TIME"
]);

export const usageTotalsSchema = z.object({
  requests: z.number().int(),
  completed: z.number().int(),
  cancelled: z.number().int(),
  failed: z.number().int(),
  inputTokens: z.number().int(),
  outputTokens: z.number().int(),
  totalTokens: z.number().int(),
  averageLatencyMs: z.number().nullable(),
  estimatedTokenRequests: z.number().int()
});

export const usageDailyPointSchema = z.object({
  date: z.iso.date(),
  requests: z.number().int(),
  inputTokens: z.number().int(),
  outputTokens: z.number().int()
});

export const usageModelBreakdownSchema = z.object({
  model: z.string(),
  requests: z.number().int(),
  inputTokens: z.number().int(),
  outputTokens: z.number().int(),
  averageLatencyMs: z.number().nullable()
});

export const usageLocationBreakdownSchema = z.object({
  processingLocation: z.enum(["LOCAL", "REMOTE"]),
  requests: z.number().int(),
  totalTokens: z.number().int()
});

export const usageSummarySchema = z.object({
  period: usagePeriodSchema,
  from: z.iso.datetime(),
  to: z.iso.datetime(),
  totals: usageTotalsSchema,
  daily: z.array(usageDailyPointSchema),
  byModel: z.array(usageModelBreakdownSchema),
  byProcessingLocation: z.array(usageLocationBreakdownSchema)
});
