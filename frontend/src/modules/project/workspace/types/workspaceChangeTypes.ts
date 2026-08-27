import { z } from "zod";
import {
  workspaceChangeOperationSchema,
  workspaceChangeSchema,
  workspaceChangeStatusSchema
} from "../schemas/workspaceChangeSchemas";

export type WorkspaceChangeOperation = z.infer<typeof workspaceChangeOperationSchema>;
export type WorkspaceChangeStatus = z.infer<typeof workspaceChangeStatusSchema>;
export type WorkspaceChange = z.infer<typeof workspaceChangeSchema>;
