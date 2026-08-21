import type { z } from "zod";
import type { knowledgeWorkspaceSchema } from "../schemas/knowledgeWorkspaceSchema";

export type KnowledgeWorkspace = z.infer<typeof knowledgeWorkspaceSchema>;
