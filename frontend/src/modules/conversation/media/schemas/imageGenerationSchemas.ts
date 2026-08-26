import { z } from "zod";

export const imageGenerationJobSchema = z.object({
  id: z.uuid(),
  conversationId: z.uuid(),
  prompt: z.string(),
  status: z.enum(["QUEUED", "GENERATING", "COMPLETED", "FAILED", "CANCELLED"]),
  provider: z.string(),
  model: z.string().nullable(),
  progress: z.number().int().min(0).max(100).nullable(),
  etaSeconds: z.number().int().nonnegative().nullable(),
  errorCode: z.string().nullable(),
  contentUrl: z.string().nullable(),
  startedAt: z.iso.datetime().nullable(),
  completedAt: z.iso.datetime().nullable(),
  createdAt: z.iso.datetime(),
  updatedAt: z.iso.datetime()
});

export const imageRuntimeSchema = z.object({
  provider: z.string(),
  configured: z.boolean(),
  available: z.boolean(),
  model: z.string().nullable(),
  message: z.string()
});
