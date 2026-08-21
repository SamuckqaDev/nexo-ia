import { z } from "zod";

/**
 * The minimal backend Workspace (owner + name) a Knowledge Vault can target with scope "workspace" —
 * distinct from the client-local, IndexedDB-backed `project/workspace` module. See D-026.
 */
export const knowledgeWorkspaceSchema = z.object({
  id: z.uuid(),
  name: z.string(),
  createdAt: z.iso.datetime(),
  updatedAt: z.iso.datetime()
});
