import { z } from "zod";

export const knowledgeGraphNodeKindSchema = z.enum(["VAULT", "SOURCE", "CHUNK"]);
export const knowledgeGraphRelationSchema = z.enum(["CONTAINS", "SEMANTIC"]);

export const knowledgeGraphNodeSchema = z.object({
  id: z.string(),
  kind: knowledgeGraphNodeKindSchema,
  vaultId: z.uuid(),
  sourceId: z.uuid().nullable(),
  ordinal: z.number().int().nullable(),
  label: z.string(),
  detail: z.string(),
  excerpt: z.string().nullable(),
  status: z.string()
});

export const knowledgeGraphEdgeSchema = z.object({
  id: z.string(),
  relation: knowledgeGraphRelationSchema,
  fromId: z.string(),
  toId: z.string(),
  similarity: z.number().nullable()
});

export const knowledgeGraphSchema = z.object({
  nodes: z.array(knowledgeGraphNodeSchema),
  edges: z.array(knowledgeGraphEdgeSchema),
  vaultCount: z.number().int().nonnegative(),
  sourceCount: z.number().int().nonnegative(),
  chunkCount: z.number().int().nonnegative(),
  truncated: z.boolean()
});
