import { z } from "zod";

export const projectWorkspaceSchema = z.object({
  id: z.uuid(),
  ownerId: z.uuid(),
  name: z.string().trim().min(1),
  directoryName: z.string().trim().min(1),
  access: z.enum(["read", "propose", "commands"]),
  platform: z.enum(["windows", "macos", "linux", "unknown"]),
  source: z.literal("local-directory"),
  branch: z.string().optional(),
  addedAt: z.iso.datetime()
});
