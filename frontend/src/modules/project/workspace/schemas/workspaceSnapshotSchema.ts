import { z } from "zod";
import { projectWorkspaceSchema } from "./projectWorkspaceSchema";

export const workspaceSnapshotSchema = z.object({
  capturedAt: z.iso.datetime(),
  entries: z.array(z.object({
    path: z.string(),
    kind: z.enum(["directory", "file"]),
    size: z.number().nonnegative().nullable(),
    lastModified: z.number().nonnegative().nullable()
  })),
  truncated: z.boolean(),
  scan: z.object({
    maxEntries: z.number().int().positive(),
    maxDepth: z.number().int().positive(),
    omissionCount: z.number().int().nonnegative(),
    omissions: z.array(z.object({
      path: z.string(),
      reason: z.enum(["depth-limit", "entry-limit", "ignored-directory", "unreadable-entry"])
    }))
  }).optional()
});

const directoryHandleSchema = z.custom<FileSystemDirectoryHandle>((value: unknown): boolean => {
  if (typeof value !== "object" || value === null) return false;
  return "kind" in value && value.kind === "directory" && "name" in value && typeof value.name === "string";
}, "The saved directory permission is no longer available.");

export const storedWorkspaceRecordSchema = z.object({
  key: z.string(),
  ownerId: z.uuid(),
  workspace: projectWorkspaceSchema,
  directoryHandle: directoryHandleSchema,
  snapshot: workspaceSnapshotSchema,
  pendingSnapshot: workspaceSnapshotSchema.nullable()
});

export const storedWorkspaceSelectionSchema = z.object({
  ownerId: z.uuid(),
  activeWorkspaceId: z.uuid().nullable()
});
