import { z } from "zod";

export const backendVaultScopeSchema = z.enum(["PERSONAL", "WORKSPACE", "PROJECT", "TEAM", "ORGANIZATION"]);

export const backendVaultSchema = z.object({
  id: z.uuid(),
  name: z.string(),
  description: z.string().nullable(),
  scope: backendVaultScopeSchema,
  workspaceId: z.uuid().nullable(),
  createdAt: z.iso.datetime(),
  updatedAt: z.iso.datetime()
});
