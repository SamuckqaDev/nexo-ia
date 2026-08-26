import { z } from "zod";

export const backendVaultScopeSchema = z.enum(["PERSONAL", "WORKSPACE", "PROJECT", "TEAM", "ORGANIZATION"]);

export const backendVaultSchema = z.object({
  id: z.uuid(),
  name: z.string(),
  description: z.string().nullable(),
  scope: backendVaultScopeSchema,
  workspaceId: z.uuid().nullable(),
  ownerId: z.uuid(),
  ownerType: z.enum(["USER", "TEAM"]),
  ownerName: z.string(),
  manageable: z.boolean(),
  writable: z.boolean(),
  createdAt: z.iso.datetime(),
  updatedAt: z.iso.datetime()
});
