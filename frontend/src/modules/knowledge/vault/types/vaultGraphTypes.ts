import type { KnowledgeVault } from "./vaultTypes";

export type VaultGraphNodeKind = "vault" | "source";
export type VaultGraphRelation = "contains" | "related";

export type VaultGraphNode = {
  id: string;
  kind: VaultGraphNodeKind;
  vaultId: string;
  sourceId?: string;
  label: string;
  detail: string;
  x: number;
  y: number;
  attached: boolean;
};

export type VaultGraphEdge = {
  id: string;
  relation: VaultGraphRelation;
  fromId: string;
  toId: string;
  x1: number;
  y1: number;
  x2: number;
  y2: number;
};

export type VaultKnowledgeGraph = {
  width: number;
  height: number;
  nodes: VaultGraphNode[];
  edges: VaultGraphEdge[];
};

export type VaultKnowledgeGraphProps = {
  vaults: KnowledgeVault[];
  attachedSourceIds: string[];
  selectedVaultId?: string;
  selectedSourceId?: string | null;
  onSelectVault: (vaultId: string) => void;
  onSelectSource: (vaultId: string, sourceId: string) => void;
};
