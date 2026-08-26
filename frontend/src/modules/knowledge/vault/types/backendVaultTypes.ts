import type { z } from "zod";
import type { backendVaultScopeSchema, backendVaultSchema } from "../schemas/vaultSchemas";
import type {
  backendSourceSchema,
  sourceIngestionStatusSchema,
  sourceStatusSchema
} from "../schemas/sourceSchemas";

export type BackendVaultScope = z.infer<typeof backendVaultScopeSchema>;
export type BackendVault = z.infer<typeof backendVaultSchema>;
export type SourceStatus = z.infer<typeof sourceStatusSchema>;
export type BackendSource = z.infer<typeof backendSourceSchema>;
export type SourceIngestionStatus = z.infer<typeof sourceIngestionStatusSchema>;

export type CreateVaultInput = {
  name: string;
  description?: string;
  scope: BackendVaultScope;
  workspaceId?: string;
};

export type CreateTeamVaultInput = {
  teamId: string;
  name: string;
  description: string;
};
