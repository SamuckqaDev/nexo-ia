import { z } from "zod";

export const workspaceChangeOperationSchema = z.enum(["CREATE", "EDIT", "DELETE"]);
export const workspaceChangeStatusSchema = z.enum([
  "PENDING_APPROVAL",
  "APPLIED",
  "DENIED",
  "INVALIDATED",
  "REVERTED",
  "FAILED"
]);

export const workspaceChangeSchema = z.object({
  id: z.string().uuid(),
  workspaceId: z.string().uuid(),
  operation: workspaceChangeOperationSchema,
  status: workspaceChangeStatusSchema,
  path: z.string(),
  beforeSha256: z.string().nullable(),
  afterSha256: z.string().nullable(),
  replacementCount: z.number().int().nullable(),
  beforeContent: z.string().nullable(),
  afterContent: z.string().nullable(),
  previewTruncated: z.boolean(),
  failureCode: z.string().nullable(),
  createdAt: z.string(),
  appliedAt: z.string().nullable(),
  revertedAt: z.string().nullable()
});
