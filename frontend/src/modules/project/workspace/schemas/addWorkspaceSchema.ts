import { z } from "zod";

export const addWorkspaceSchema = z.object({
  access: z.enum(["read", "propose", "commands"])
});
