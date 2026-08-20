import { z } from "zod";

export const createVaultSchema = z.object({
  name: z.string().trim().min(3, "Give the Vault a clear name."),
  description: z.string().trim().min(8, "Explain which knowledge belongs here."),
  scope: z.enum(["personal", "project", "team", "organization"])
});
