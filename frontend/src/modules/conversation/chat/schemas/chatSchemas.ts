import { z } from "zod";
import { knowledgeCitationSchema } from "../../../knowledge/vault/schemas/citationSchema";

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

export const conversationModeSchema = z.enum(["CHAT", "AGENT"]);

export const agentStateSchema = z.enum([
  "PLANNING",
  "RUNNING",
  "VERIFYING",
  "COMPLETED",
  "FAILED",
  "CANCELLED",
  "BLOCKED"
]);

export const toolExecutionStatusSchema = z.enum([
  "RUNNING",
  "FOUND",
  "NO_RESULTS",
  "UNAVAILABLE",
  "DENIED"
]);

export const toolExecutionSchema = z.object({
  id: z.uuid(),
  toolName: z.string(),
  status: toolExecutionStatusSchema,
  durationMs: z.number().int().nullable(),
  citations: z.array(knowledgeCitationSchema),
  startedAt: z.iso.datetime(),
  completedAt: z.iso.datetime().nullable()
});

export const conversationSchema = z.object({
  id: z.uuid(),
  title: z.string(),
  providerConfigurationId: z.uuid().nullable(),
  selectedModel: z.string().nullable(),
  knowledgeVaultIds: z.array(z.uuid()).default([]),
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
  totalTokens: z.number().int().nullable(),
  contextTokensUsed: z.number().int().nullable(),
  contextTokenBudget: z.number().int().nullable(),
  tokenSource: tokenSourceSchema.nullable(),
  latencyMs: z.number().int().nullable(),
  processingLocation: processingLocationSchema.nullable(),
  failureCode: z.string().nullable(),
  mode: conversationModeSchema.optional(),
  agentState: agentStateSchema.nullable().optional(),
  createdAt: z.iso.datetime(),
  completedAt: z.iso.datetime().nullable(),
  citations: z.array(knowledgeCitationSchema).nullable(),
  toolExecutions: z.array(toolExecutionSchema).optional()
});

export const conversationTitleSchema = z.string().trim().min(1, "Name this conversation").max(160);
