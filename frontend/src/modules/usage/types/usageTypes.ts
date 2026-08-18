import type { z } from "zod";
import type {
  usageDailyPointSchema,
  usageLocationBreakdownSchema,
  usageModelBreakdownSchema,
  usagePeriodSchema,
  usageSummarySchema,
  usageTotalsSchema
} from "../schemas/usageSchemas";

export type UsagePeriod = z.infer<typeof usagePeriodSchema>;
export type UsageTotals = z.infer<typeof usageTotalsSchema>;
export type UsageDailyPoint = z.infer<typeof usageDailyPointSchema>;
export type UsageModelBreakdown = z.infer<typeof usageModelBreakdownSchema>;
export type UsageLocationBreakdown = z.infer<typeof usageLocationBreakdownSchema>;
export type UsageSummary = z.infer<typeof usageSummarySchema>;
