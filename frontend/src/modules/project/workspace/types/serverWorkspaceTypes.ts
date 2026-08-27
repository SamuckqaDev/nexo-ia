import type { z } from "zod";
import type {
  serverWorkspaceSchema,
  serverWorkspaceStatusSchema,
  serverWorkspaceTreeSchema,
  workspaceBindingSchema,
  workspaceAccessModeSchema,
  workspaceStorageTypeSchema
} from "../schemas/serverWorkspaceSchemas";

export type ServerWorkspace = z.infer<typeof serverWorkspaceSchema>;
export type ServerWorkspaceStatus = z.infer<typeof serverWorkspaceStatusSchema>;
export type ServerWorkspaceTree = z.infer<typeof serverWorkspaceTreeSchema>;
export type WorkspaceBinding = z.infer<typeof workspaceBindingSchema>;
export type ServerWorkspaceStorageType = z.infer<typeof workspaceStorageTypeSchema>;
export type ServerWorkspaceAccessMode = z.infer<typeof workspaceAccessModeSchema>;

export type CreateServerWorkspaceInput = {
  name: string;
  storageType: ServerWorkspaceStorageType;
  accessMode: ServerWorkspaceAccessMode;
  relativePath?: string;
};
