import type { UseQueryResult } from "@tanstack/react-query";
import type { z } from "zod";
import type {
  knowledgeGraphEdgeSchema,
  knowledgeGraphNodeKindSchema,
  knowledgeGraphNodeSchema,
  knowledgeGraphRelationSchema,
  knowledgeGraphSchema
} from "../schemas/knowledgeGraphSchemas";

export type KnowledgeGraphNodeKind = z.infer<typeof knowledgeGraphNodeKindSchema>;
export type KnowledgeGraphRelation = z.infer<typeof knowledgeGraphRelationSchema>;
export type KnowledgeGraphNode = z.infer<typeof knowledgeGraphNodeSchema>;
export type KnowledgeGraphEdge = z.infer<typeof knowledgeGraphEdgeSchema>;
export type BackendKnowledgeGraph = z.infer<typeof knowledgeGraphSchema>;

export type VaultGraphNode = KnowledgeGraphNode & {
  x: number;
  y: number;
};

export type VaultGraphEdge = KnowledgeGraphEdge & {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
};

export type VaultKnowledgeGraphLayout = {
  width: number;
  height: number;
  nodes: VaultGraphNode[];
  edges: VaultGraphEdge[];
};

export type VaultKnowledgeGraphProps = {
  graph: BackendKnowledgeGraph;
  selectedVaultId?: string | null;
  onSelectVault: (vaultId: string) => void;
};

export type VaultWorkbenchPosition = { x: number; y: number };

export type VaultWorkbenchDragSnapshot = {
  pointerId: number;
  pointerX: number;
  pointerY: number;
  position: VaultWorkbenchPosition;
  minX: number;
  maxX: number;
  minY: number;
  maxY: number;
};

export type VaultWorkbenchModalProps = {
  open: boolean;
  onClose: () => void;
  graphQuery: UseQueryResult<BackendKnowledgeGraph>;
  selectedVaultId?: string | null;
  onSelectVault: (vaultId: string) => void;
};
