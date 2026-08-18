import { z } from "zod";
import { processingLocationSchema, tokenSourceSchema } from "./chatSchemas";

/**
 * Server-Sent Event payloads are untrusted at runtime, so every frame is validated before it can
 * change what the user sees.
 */
export const startedEventSchema = z.object({
  userMessageId: z.uuid(),
  assistantMessageId: z.uuid(),
  correlationId: z.uuid(),
  model: z.string(),
  processingLocation: processingLocationSchema
});

export const tokenEventSchema = z.object({
  content: z.string(),
  index: z.number().int().nonnegative()
});

export const usageEventSchema = z.object({
  inputTokens: z.number().int().nullable(),
  outputTokens: z.number().int().nullable(),
  tokenSource: tokenSourceSchema.nullable(),
  latencyMs: z.number().int()
});

export const completedEventSchema = z.object({
  messageId: z.uuid(),
  content: z.string(),
  completedAt: z.iso.datetime()
});

export const cancelledEventSchema = z.object({
  messageId: z.uuid(),
  content: z.string(),
  cancelledAt: z.iso.datetime()
});

export const streamErrorEventSchema = z.object({
  messageId: z.uuid(),
  code: z.string(),
  message: z.string()
});

export const streamEventSchema = z.discriminatedUnion("event", [
  z.object({ event: z.literal("started"), data: startedEventSchema }),
  z.object({ event: z.literal("token"), data: tokenEventSchema }),
  z.object({ event: z.literal("usage"), data: usageEventSchema }),
  z.object({ event: z.literal("completed"), data: completedEventSchema }),
  z.object({ event: z.literal("cancelled"), data: cancelledEventSchema }),
  z.object({ event: z.literal("error"), data: streamErrorEventSchema })
]);
