import { z } from "zod";

export const messageStatusSchema = z.enum([
  "QUEUED",
  "STREAMING",
  "CANCELLING",
  "COMPLETED",
  "CANCELLED",
  "FAILED"
]);

export const tokenSourceSchema = z.enum(["PROVIDER", "ESTIMATE"]);

export const processingLocationSchema = z.enum(["LOCAL", "REMOTE"]);

export const conversationSchema = z.object({
  id: z.uuid(),
  title: z.string(),
  providerConfigurationId: z.uuid().nullable(),
  selectedModel: z.string().nullable(),
  createdAt: z.iso.datetime(),
  updatedAt: z.iso.datetime()
});

export const conversationMessageSchema = z.object({
  id: z.uuid(),
  role: z.enum(["USER", "ASSISTANT"]),
  status: messageStatusSchema,
  content: z.string(),
  model: z.string().nullable(),
  inputTokens: z.number().int().nullable(),
  outputTokens: z.number().int().nullable(),
  tokenSource: tokenSourceSchema.nullable(),
  latencyMs: z.number().int().nullable(),
  processingLocation: processingLocationSchema.nullable(),
  failureCode: z.string().nullable(),
  createdAt: z.iso.datetime(),
  completedAt: z.iso.datetime().nullable()
});

export const conversationTitleSchema = z.string().trim().min(1, "Name this conversation").max(160);
