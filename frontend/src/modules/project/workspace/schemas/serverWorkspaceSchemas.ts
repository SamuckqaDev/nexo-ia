import { z } from "zod";

export const workspaceStorageTypeSchema = z.enum(["UNBOUND", "MANAGED", "MOUNTED"]);
export const workspaceAccessModeSchema = z.enum([
  "READ_ONLY",
  "WRITE_WITH_APPROVAL",
  "COMMANDS_WITH_APPROVAL"
]);
export const workspaceStatusSchema = z.enum([
  "UNBOUND",
  "AVAILABLE",
  "MISSING",
  "CHANGED",
  "LOCKED",
  "ERROR"
]);

export const serverWorkspaceSchema = z.object({
  id: z.uuid(),
  name: z.string(),
  storageType: workspaceStorageTypeSchema,
  accessMode: workspaceAccessModeSchema,
  status: workspaceStatusSchema,
  relativePath: z.string().nullable(),
  lastScannedAt: z.iso.datetime().nullable(),
  createdAt: z.iso.datetime(),
  updatedAt: z.iso.datetime()
});

export const workspaceGitSummarySchema = z.object({
  branch: z.string().nullable(),
  head: z.string().nullable(),
  detached: z.boolean()
});

export const serverWorkspaceStatusSchema = z.object({
  status: workspaceStatusSchema,
  storageType: workspaceStorageTypeSchema,
  accessMode: workspaceAccessModeSchema,
  relativePath: z.string().nullable(),
  structureFingerprint: z.string().nullable(),
  lastScannedAt: z.iso.datetime().nullable(),
  git: workspaceGitSummarySchema.nullable(),
  detectedStack: z.array(z.string()),
  reason: z.string().nullable()
});

export const workspaceTreeEntrySchema = z.object({
  path: z.string(),
  name: z.string(),
  type: z.enum(["DIRECTORY", "FILE"]),
  sizeBytes: z.number().int().nullable(),
  modifiedAt: z.iso.datetime().nullable()
});

export const serverWorkspaceTreeSchema = z.object({
  path: z.string(),
  entries: z.array(workspaceTreeEntrySchema),
  omissions: z.array(z.object({ name: z.string(), reason: z.string() })),
  truncated: z.boolean(),
  nextCursor: z.string().nullable()
});

export const workspaceBindingSchema = z.object({
  id: z.uuid(),
  workspaceId: z.uuid(),
  deviceId: z.uuid(),
  deviceName: z.string(),
  deviceStatus: z.enum(["OFFLINE", "ONLINE", "REVOKED"]),
  displayName: z.string(),
  status: z.enum(["AVAILABLE", "CHANGED", "OFFLINE", "MISSING", "ERROR"]),
  structureFingerprint: z.string().nullable(),
  gitHead: z.string().nullable(),
  gitBranch: z.string().nullable(),
  lastSeenAt: z.iso.datetime().nullable(),
  createdAt: z.iso.datetime(),
  updatedAt: z.iso.datetime()
});
