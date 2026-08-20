import type {
  VaultGraphEdge,
  VaultGraphNode,
  VaultKnowledgeGraph
} from "../types/vaultGraphTypes";
import type { KnowledgeVault, VaultSource } from "../types/vaultTypes";

const CANVAS_MIN_WIDTH = 720;
const VAULT_COLUMN_WIDTH = 320;
const VAULT_Y = 82;
const SOURCE_START_Y = 194;
const SOURCE_ROW_HEIGHT = 104;
const MAX_RELATED_EDGES = 24;

const ignoredTerms = new Set([
  "about", "after", "also", "and", "como", "com", "das", "data", "de", "does", "dos", "each",
  "for", "from", "into", "its", "local", "mais", "never", "not", "para", "pela", "pelo", "que",
  "remain", "should", "the", "this", "uma", "when", "with"
]);

function nodeId(kind: "vault" | "source", id: string): string {
  return `${kind}:${id}`;
}

function normalizeTerm(value: string): string {
  const normalized: string = value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]/g, "");
  return normalized.length > 4 && normalized.endsWith("s") ? normalized.slice(0, -1) : normalized;
}

function sourceTerms(source: VaultSource): Set<string> {
  const text: string = `${source.name} ${source.contentPreview ?? ""}`;
  return new Set(text
    .split(/\s+/)
    .map(normalizeTerm)
    .filter((term: string): boolean => term.length >= 4 && !ignoredTerms.has(term)));
}

function sharedTermCount(left: Set<string>, right: Set<string>): number {
  let matches = 0;
  left.forEach((term: string): void => {
    if (right.has(term)) matches += 1;
  });
  return matches;
}

export function buildVaultKnowledgeGraph(
  vaults: KnowledgeVault[],
  attachedSourceIds: string[]
): VaultKnowledgeGraph {
  const vaultCount: number = Math.max(vaults.length, 1);
  const width: number = Math.max(CANVAS_MIN_WIDTH, vaultCount * VAULT_COLUMN_WIDTH);
  const columnWidth: number = width / vaultCount;
  const maxSourceRows: number = Math.max(1, ...vaults.map((vault: KnowledgeVault): number => Math.ceil(vault.sources.length / 2)));
  const height: number = Math.max(360, SOURCE_START_Y + maxSourceRows * SOURCE_ROW_HEIGHT);
  const nodes: VaultGraphNode[] = [];
  const edges: VaultGraphEdge[] = [];

  vaults.forEach((vault: KnowledgeVault, vaultIndex: number): void => {
    const centerX: number = columnWidth * vaultIndex + columnWidth / 2;
    const vaultNode: VaultGraphNode = {
      id: nodeId("vault", vault.id),
      kind: "vault",
      vaultId: vault.id,
      label: vault.name,
      detail: `${vault.sources.length} source${vault.sources.length === 1 ? "" : "s"}`,
      x: centerX,
      y: VAULT_Y,
      attached: vault.sources.some((source: VaultSource): boolean => attachedSourceIds.includes(source.id))
    };
    nodes.push(vaultNode);

    vault.sources.forEach((source: VaultSource, sourceIndex: number): void => {
      const columnOffset: number = sourceIndex % 2 === 0 ? -82 : 82;
      const sourceNode: VaultGraphNode = {
        id: nodeId("source", source.id),
        kind: "source",
        vaultId: vault.id,
        sourceId: source.id,
        label: source.name,
        detail: `${source.type} · ${source.size}`,
        x: centerX + columnOffset,
        y: SOURCE_START_Y + Math.floor(sourceIndex / 2) * SOURCE_ROW_HEIGHT,
        attached: attachedSourceIds.includes(source.id)
      };
      nodes.push(sourceNode);
      edges.push({
        id: `contains:${vault.id}:${source.id}`,
        relation: "contains",
        fromId: vaultNode.id,
        toId: sourceNode.id,
        x1: vaultNode.x,
        y1: vaultNode.y,
        x2: sourceNode.x,
        y2: sourceNode.y
      });
    });
  });

  const sourceNodes: VaultGraphNode[] = nodes.filter((node: VaultGraphNode): boolean => node.kind === "source");
  const sourceLookup = new Map<string, VaultSource>();
  vaults.forEach((vault: KnowledgeVault): void => vault.sources.forEach((source: VaultSource): void => {
    sourceLookup.set(source.id, source);
  }));
  let relatedEdges = 0;

  for (let leftIndex = 0; leftIndex < sourceNodes.length && relatedEdges < MAX_RELATED_EDGES; leftIndex += 1) {
    const leftNode: VaultGraphNode = sourceNodes[leftIndex];
    const leftSource: VaultSource | undefined = leftNode.sourceId ? sourceLookup.get(leftNode.sourceId) : undefined;
    if (!leftSource) continue;
    const leftTerms: Set<string> = sourceTerms(leftSource);

    for (let rightIndex = leftIndex + 1; rightIndex < sourceNodes.length && relatedEdges < MAX_RELATED_EDGES; rightIndex += 1) {
      const rightNode: VaultGraphNode = sourceNodes[rightIndex];
      if (leftNode.vaultId === rightNode.vaultId) continue;
      const rightSource: VaultSource | undefined = rightNode.sourceId ? sourceLookup.get(rightNode.sourceId) : undefined;
      if (!rightSource || sharedTermCount(leftTerms, sourceTerms(rightSource)) < 2) continue;
      edges.push({
        id: `related:${leftNode.id}:${rightNode.id}`,
        relation: "related",
        fromId: leftNode.id,
        toId: rightNode.id,
        x1: leftNode.x,
        y1: leftNode.y,
        x2: rightNode.x,
        y2: rightNode.y
      });
      relatedEdges += 1;
    }
  }

  return { width, height, nodes, edges };
}
