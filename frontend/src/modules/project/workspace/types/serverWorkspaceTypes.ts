import type { z } from "zod";
import type {
  serverWorkspaceSchema,
  serverWorkspaceStatusSchema,
  serverWorkspaceTreeSchema,
  workspaceAccessModeSchema,
  workspaceStorageTypeSchema
} from "../schemas/serverWorkspaceSchemas";

export type ServerWorkspace = z.infer<typeof serverWorkspaceSchema>;
export type ServerWorkspaceStatus = z.infer<typeof serverWorkspaceStatusSchema>;
export type ServerWorkspaceTree = z.infer<typeof serverWorkspaceTreeSchema>;
export type ServerWorkspaceStorageType = z.infer<typeof workspaceStorageTypeSchema>;
export type ServerWorkspaceAccessMode = z.infer<typeof workspaceAccessModeSchema>;

export type CreateServerWorkspaceInput = {
  name: string;
  storageType: Exclude<ServerWorkspaceStorageType, "UNBOUND">;
  accessMode: ServerWorkspaceAccessMode;
  relativePath?: string;
};
