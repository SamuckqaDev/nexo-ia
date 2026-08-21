import { z } from "zod";

export const sourceStatusSchema = z.enum(["REGISTERED", "INGESTING", "READY", "FAILED", "UNSUPPORTED"]);

export const backendSourceSchema = z.object({
  id: z.uuid(),
  vaultId: z.uuid(),
  sourceKind: z.enum(["UPLOAD"]),
  displayName: z.string(),
  mimeType: z.string(),
  byteSize: z.number().int(),
  status: sourceStatusSchema,
  errorCode: z.string().nullable(),
  createdAt: z.iso.datetime(),
  updatedAt: z.iso.datetime()
});

export const sourceIngestionStatusSchema = z.object({
  status: sourceStatusSchema,
  errorCode: z.string().nullable(),
  chunkCount: z.number().int(),
  contentHash: z.string(),
  byteSize: z.number().int(),
  mimeType: z.string()
});
