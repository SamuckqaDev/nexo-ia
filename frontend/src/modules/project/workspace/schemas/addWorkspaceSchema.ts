import { z } from "zod";

export const addWorkspaceSchema = z.object({
  name: z.string().trim().min(2, "Give this workspace a recognizable name."),
  path: z.string().trim().min(2, "Enter the exact folder path."),
  access: z.enum(["read", "propose", "commands"])
});
