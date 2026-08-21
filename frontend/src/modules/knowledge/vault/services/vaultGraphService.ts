import type {
  BackendKnowledgeGraph,
  KnowledgeGraphEdge,
  KnowledgeGraphNode,
  VaultGraphEdge,
  VaultGraphNode,
  VaultKnowledgeGraphLayout
} from "../types/vaultGraphTypes";

const MIN_CLUSTER_SIZE = 720;
const MIN_WIDTH = 960;
const MIN_HEIGHT = 680;
const SOURCE_RADIUS = 155;
const CHUNK_START_RADIUS = 290;
const CHUNKS_PER_RING = 10;
const CHUNK_RING_GAP = 72;

type Point = { x: number; y: number };

function pointOnCircle(center: Point, radius: number, angle: number): Point {
  return {
    x: center.x + Math.cos(angle) * radius,
    y: center.y + Math.sin(angle) * radius
  };
}

function sourceSemanticEdges(graph: BackendKnowledgeGraph): KnowledgeGraphEdge[] {
  const nodeById = new Map<string, KnowledgeGraphNode>(graph.nodes.map((node) => [node.id, node]));
  const strongestByPair = new Map<string, KnowledgeGraphEdge>();

  graph.edges.filter((edge) => edge.relation === "SEMANTIC").forEach((edge) => {
    const left: KnowledgeGraphNode | undefined = nodeById.get(edge.fromId);
    const right: KnowledgeGraphNode | undefined = nodeById.get(edge.toId);
    if (!left?.sourceId || !right?.sourceId || left.sourceId === right.sourceId) return;
    const orderedIds: string[] = [left.sourceId, right.sourceId].sort();
    const pairId: string = orderedIds.join(":");
    const existing: KnowledgeGraphEdge | undefined = strongestByPair.get(pairId);
    if (existing && (existing.similarity ?? 0) >= (edge.similarity ?? 0)) return;
    strongestByPair.set(pairId, {
      id: `semantic-source:${pairId}`,
      relation: "SEMANTIC",
      fromId: `source:${orderedIds[0]}`,
      toId: `source:${orderedIds[1]}`,
      similarity: edge.similarity
    });
  });

  return [...strongestByPair.values()];
}

export function buildVaultKnowledgeGraph(
  graph: BackendKnowledgeGraph,
  includeChunks: boolean
): VaultKnowledgeGraphLayout {
  const vaultNodes: KnowledgeGraphNode[] = graph.nodes.filter((node) => node.kind === "VAULT");
  const sourceNodes: KnowledgeGraphNode[] = graph.nodes.filter((node) => node.kind === "SOURCE");
  const chunkNodes: KnowledgeGraphNode[] = includeChunks
    ? graph.nodes.filter((node) => node.kind === "CHUNK")
    : [];
  const maxChunksPerSource: number = Math.max(0, ...sourceNodes.map((source) =>
    chunkNodes.filter((chunk) => chunk.sourceId === source.sourceId).length));
  const chunkRings: number = Math.max(1, Math.ceil(maxChunksPerSource / CHUNKS_PER_RING));
  const clusterRadius: number = includeChunks
    ? CHUNK_START_RADIUS + (chunkRings - 1) * CHUNK_RING_GAP + 90
    : SOURCE_RADIUS + 130;
  const clusterSize: number = Math.max(MIN_CLUSTER_SIZE, clusterRadius * 2);
  const columns: number = Math.max(1, Math.ceil(Math.sqrt(Math.max(vaultNodes.length, 1))));
  const rows: number = Math.max(1, Math.ceil(Math.max(vaultNodes.length, 1) / columns));
  const width: number = Math.max(MIN_WIDTH, columns * clusterSize);
  const height: number = Math.max(MIN_HEIGHT, rows * clusterSize);
  const positions = new Map<string, Point>();

  vaultNodes.forEach((vault, vaultIndex) => {
    const column: number = vaultIndex % columns;
    const row: number = Math.floor(vaultIndex / columns);
    const center: Point = {
      x: column * clusterSize + clusterSize / 2 + (width - columns * clusterSize) / 2,
      y: row * clusterSize + clusterSize / 2 + (height - rows * clusterSize) / 2
    };
    positions.set(vault.id, center);

    const vaultSources: KnowledgeGraphNode[] = sourceNodes.filter((source) => source.vaultId === vault.vaultId);
    const sourceCount: number = Math.max(vaultSources.length, 1);
    vaultSources.forEach((source, sourceIndex) => {
      const sourceAngle: number = -Math.PI / 2 + (Math.PI * 2 * sourceIndex) / sourceCount;
      positions.set(source.id, pointOnCircle(center, SOURCE_RADIUS, sourceAngle));

      const sourceChunks: KnowledgeGraphNode[] = chunkNodes.filter((chunk) => chunk.sourceId === source.sourceId);
      sourceChunks.forEach((chunk, chunkIndex) => {
        const ring: number = Math.floor(chunkIndex / CHUNKS_PER_RING);
        const ringStart: number = ring * CHUNKS_PER_RING;
        const ringCount: number = Math.min(CHUNKS_PER_RING, sourceChunks.length - ringStart);
        const slot: number = chunkIndex - ringStart;
        const sector: number = (Math.PI * 2) / sourceCount;
        const spread: number = Math.min(sector * 0.72, Math.PI * 0.72);
        const offset: number = ringCount === 1 ? 0 : -spread / 2 + (spread * slot) / (ringCount - 1);
        positions.set(
          chunk.id,
          pointOnCircle(center, CHUNK_START_RADIUS + ring * CHUNK_RING_GAP, sourceAngle + offset)
        );
      });
    });
  });

  const visibleNodes: KnowledgeGraphNode[] = [...vaultNodes, ...sourceNodes, ...chunkNodes];
  const layoutNodes: VaultGraphNode[] = visibleNodes.flatMap((node) => {
    const point: Point | undefined = positions.get(node.id);
    return point ? [{ ...node, ...point }] : [];
  });
  const visibleNodeIds = new Set(layoutNodes.map((node) => node.id));
  const graphEdges: KnowledgeGraphEdge[] = includeChunks
    ? graph.edges
    : [
        ...graph.edges.filter((edge) => edge.relation === "CONTAINS"
          && edge.fromId.startsWith("vault:") && edge.toId.startsWith("source:")),
        ...sourceSemanticEdges(graph)
      ];
  const layoutEdges: VaultGraphEdge[] = graphEdges.flatMap((edge) => {
    if (!visibleNodeIds.has(edge.fromId) || !visibleNodeIds.has(edge.toId)) return [];
    const from: Point | undefined = positions.get(edge.fromId);
    const to: Point | undefined = positions.get(edge.toId);
    return from && to ? [{ ...edge, x1: from.x, y1: from.y, x2: to.x, y2: to.y }] : [];
  });

  return { width, height, nodes: layoutNodes, edges: layoutEdges };
}
