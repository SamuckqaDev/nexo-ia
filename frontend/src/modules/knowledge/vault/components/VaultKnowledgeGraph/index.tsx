import {
  Buildings,
  FileText,
  FolderOpen,
  MagnifyingGlass,
  Minus,
  Plus,
  Stack
} from "@phosphor-icons/react";
import { useMemo, useState, type ReactElement } from "react";
import { buildVaultKnowledgeGraph } from "../../services/vaultGraphService";
import type {
  KnowledgeGraphNode,
  VaultGraphEdge,
  VaultGraphNode,
  VaultKnowledgeGraphLayout,
  VaultKnowledgeGraphProps
} from "../../types/vaultGraphTypes";
import {
  Edge,
  EdgeLayer,
  EmptyGraph,
  GraphActions,
  GraphCanvas,
  GraphNode,
  GraphScene,
  GraphSearch,
  GraphShell,
  GraphStage,
  GraphSummary,
  GraphToolbar,
  GraphViewport,
  Inspector,
  InspectorBadge,
  InspectorCopy,
  Legend,
  LegendItem,
  ToolButton
} from "./styles";

const MIN_ZOOM = 0.55;
const MAX_ZOOM = 1.35;
const ZOOM_STEP = 0.1;

const clampZoom = (value: number): number => Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, value));

export function VaultKnowledgeGraph({
  graph,
  selectedVaultId,
  onSelectVault
}: VaultKnowledgeGraphProps): ReactElement {
  const [includeChunks, setIncludeChunks] = useState<boolean>(true);
  const [zoom, setZoom] = useState<number>(0.82);
  const [query, setQuery] = useState<string>("");
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const layout: VaultKnowledgeGraphLayout = useMemo(
    (): VaultKnowledgeGraphLayout => buildVaultKnowledgeGraph(graph, includeChunks),
    [graph, includeChunks]
  );
  const normalizedQuery: string = query.trim().toLowerCase();
  const selectedNode: KnowledgeGraphNode | undefined = layout.nodes.find((node) => node.id === selectedNodeId);
  const semanticCount: number = layout.edges.filter((edge) => edge.relation === "SEMANTIC").length;

  if (!graph.nodes.length) {
    return <EmptyGraph>No indexed knowledge is available for this user yet.</EmptyGraph>;
  }

  const matchesSearch = (node: VaultGraphNode): boolean => !normalizedQuery
    || `${node.label} ${node.detail} ${node.ownerName} ${node.excerpt ?? ""}`.toLowerCase().includes(normalizedQuery);

  const selectNode = (node: VaultGraphNode): void => {
    setSelectedNodeId(node.id);
    onSelectVault(node.vaultId);
  };

  return (
    <GraphShell>
      <GraphToolbar>
        <GraphSearch>
          <MagnifyingGlass size={14} aria-hidden />
          <input
            value={query}
            onChange={(event): void => setQuery(event.target.value)}
            aria-label="Search the knowledge graph"
            placeholder="Search nodes"
          />
        </GraphSearch>
        <Legend aria-label="Knowledge graph legend">
          <LegendItem $kind="vault">Vault</LegendItem>
          <LegendItem $kind="team">Team Vault</LegendItem>
          <LegendItem $kind="source">Document</LegendItem>
          <LegendItem $kind="chunk">Knowledge</LegendItem>
          <LegendItem $kind="semantic">Semantic link</LegendItem>
        </Legend>
        <GraphActions>
          <ToolButton
            type="button"
            $active={includeChunks}
            aria-pressed={includeChunks}
            onClick={(): void => setIncludeChunks((current) => !current)}
          >
            <Stack size={14} /> Chunks
          </ToolButton>
          <ToolButton type="button" aria-label="Zoom out" onClick={(): void => setZoom((current) => clampZoom(current - ZOOM_STEP))}>
            <Minus size={14} />
          </ToolButton>
          <GraphSummary>{Math.round(zoom * 100)}%</GraphSummary>
          <ToolButton type="button" aria-label="Zoom in" onClick={(): void => setZoom((current) => clampZoom(current + ZOOM_STEP))}>
            <Plus size={14} />
          </ToolButton>
        </GraphActions>
      </GraphToolbar>

      <GraphStage>
        <GraphViewport tabIndex={0} aria-label="Semantic knowledge graph. Scroll to explore the network.">
          <GraphCanvas $width={layout.width * zoom} $height={layout.height * zoom}>
            <GraphScene $width={layout.width} $height={layout.height} $zoom={zoom}>
              <EdgeLayer viewBox={`0 0 ${layout.width} ${layout.height}`} aria-hidden="true">
                {layout.edges.map((edge: VaultGraphEdge) => (
                  <Edge
                    key={edge.id}
                    x1={edge.x1}
                    y1={edge.y1}
                    x2={edge.x2}
                    y2={edge.y2}
                    $semantic={edge.relation === "SEMANTIC"}
                    $strength={edge.similarity ?? 1}
                  />
                ))}
              </EdgeLayer>
              {layout.nodes.map((node: VaultGraphNode) => {
                const selected: boolean = selectedNodeId === node.id
                  || (node.kind === "VAULT" && selectedVaultId === node.vaultId && !selectedNodeId);
                return (
                  <GraphNode
                    key={node.id}
                    type="button"
                    $kind={node.kind}
                    $team={node.ownerType === "TEAM"}
                    $x={node.x}
                    $y={node.y}
                    $selected={selected}
                    $muted={!matchesSearch(node)}
                    aria-pressed={selected}
                    aria-label={`${node.kind.toLowerCase()}: ${node.label}. ${node.detail}`}
                    title={node.excerpt ?? node.detail}
                    onClick={(): void => selectNode(node)}
                  >
                    {node.kind === "VAULT"
                      ? node.ownerType === "TEAM"
                        ? <Buildings size={18} weight="duotone" />
                        : <FolderOpen size={18} weight="duotone" />
                      : node.kind === "SOURCE"
                        ? <Stack size={16} weight="duotone" />
                        : <FileText size={14} weight="duotone" />}
                    <span><strong>{node.label}</strong><small>{node.detail}</small></span>
                  </GraphNode>
                );
              })}
            </GraphScene>
          </GraphCanvas>
        </GraphViewport>

        {selectedNode && (
          <Inspector aria-live="polite">
            <header>
              <div><strong>{selectedNode.label}</strong><span>{selectedNode.detail}</span></div>
              <InspectorBadge>{selectedNode.kind.toLowerCase()}</InspectorBadge>
            </header>
            <InspectorBadge>{selectedNode.ownerType === "TEAM" ? `Team · ${selectedNode.ownerName}` : selectedNode.ownerName}</InspectorBadge>
            <InspectorCopy>
              {selectedNode.excerpt ?? (selectedNode.kind === "SOURCE"
                ? "Select one of this document's knowledge chunks to inspect its indexed excerpt."
                : "This node groups the knowledge stored below it.")}
            </InspectorCopy>
            <footer>
              <span>{selectedNode.status.toLowerCase()}</span>
              {selectedNode.ordinal !== null && <span>chunk {selectedNode.ordinal + 1}</span>}
            </footer>
          </Inspector>
        )}
      </GraphStage>

      <GraphSummary aria-live="polite">
        {layout.nodes.length} visible nodes · {semanticCount} semantic connection{semanticCount === 1 ? "" : "s"}
      </GraphSummary>
    </GraphShell>
  );
}
