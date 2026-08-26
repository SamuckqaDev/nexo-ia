import { z } from "zod";
import { backendVaultSchema } from "../../../knowledge/vault/schemas/vaultSchemas";

export const profileKeySchema = z.enum(["LOCKED", "READER", "RESEARCHER", "BUILDER", "OPERATOR"]);
export const teamRoleSchema = z.enum(["ADMIN", "MEMBER"]);

export const teamSchema = z.object({
  id: z.uuid(),
  name: z.string(),
  createdBy: z.uuid(),
  defaultProfile: profileKeySchema,
  tokenBudgetLimit: z.number().int().positive().nullable(),
  teamRole: teamRoleSchema,
  assignedProfile: profileKeySchema,
  manageable: z.boolean(),
  createdAt: z.iso.datetime(),
  updatedAt: z.iso.datetime()
});

export const teamMemberSchema = z.object({
  userId: z.uuid(),
  username: z.string(),
  name: z.string(),
  email: z.email(),
  teamRole: teamRoleSchema,
  assignedProfile: profileKeySchema,
  joinedAt: z.iso.datetime()
});

export const teamCandidateSchema = z.object({
  userId: z.uuid(),
  username: z.string(),
  name: z.string(),
  email: z.email(),
  role: z.enum(["OWNER", "ADMIN", "MEMBER"]),
  assignedProfile: profileKeySchema
});

export const createTeamSchema = z.object({
  name: z.string().trim().min(3, "Give the Team a clear name.").max(120),
  defaultProfile: profileKeySchema,
  tokenBudgetLimit: z.number().int().positive("Use a positive token budget.").optional()
});

export const addTeamMemberSchema = z.object({
  userId: z.uuid("Choose a Nexo user."),
  teamRole: teamRoleSchema,
  profile: profileKeySchema
});

export const createTeamVaultSchema = z.object({
  name: z.string().trim().min(3, "Give the Vault a clear name.").max(160),
  description: z.string().trim().min(8, "Explain which shared knowledge belongs here.").max(500)
});

export const teamVaultSchema = backendVaultSchema;
