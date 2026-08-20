import { FileText, FolderOpen, Paperclip } from "@phosphor-icons/react";
import { useMemo, type ReactElement } from "react";
import { buildVaultKnowledgeGraph } from "../../services/vaultGraphService";
import type {
  VaultGraphEdge,
  VaultGraphNode,
  VaultKnowledgeGraph as VaultKnowledgeGraphData,
  VaultKnowledgeGraphProps
} from "../../types/vaultGraphTypes";
import {
  Edge,
  EdgeLayer,
  EmptyGraph,
  GraphCanvas,
  GraphNode,
  GraphShell,
  GraphSummary,
  GraphToolbar,
  GraphViewport,
  Legend,
  LegendItem
} from "./styles";

export function VaultKnowledgeGraph({
  vaults,
  attachedSourceIds,
  selectedVaultId,
  selectedSourceId,
  onSelectVault,
  onSelectSource
}: VaultKnowledgeGraphProps): ReactElement {
  const graph: VaultKnowledgeGraphData = useMemo(
    (): VaultKnowledgeGraphData => buildVaultKnowledgeGraph(vaults, attachedSourceIds),
    [attachedSourceIds, vaults]
  );
  const relatedCount: number = graph.edges.filter((edge: VaultGraphEdge): boolean => edge.relation === "related").length;

  if (!vaults.length) {
    return <EmptyGraph>No knowledge collections are available for this user yet.</EmptyGraph>;
  }

  return (
    <GraphShell>
      <GraphToolbar>
        <Legend aria-label="Knowledge map legend">
          <LegendItem $kind="vault">Vault</LegendItem>
          <LegendItem $kind="source">Source</LegendItem>
          <LegendItem $kind="attached">In Chat context</LegendItem>
          <LegendItem $kind="related">Shared terms</LegendItem>
        </Legend>
        <GraphSummary aria-live="polite">
          {graph.nodes.length} nodes · {relatedCount} related connection{relatedCount === 1 ? "" : "s"}
        </GraphSummary>
      </GraphToolbar>
      <GraphViewport tabIndex={0} aria-label="Interactive knowledge map. Scroll inside this area to explore all nodes.">
        <GraphCanvas $width={graph.width} $height={graph.height}>
          <EdgeLayer viewBox={`0 0 ${graph.width} ${graph.height}`} aria-hidden="true">
            {graph.edges.map((edge: VaultGraphEdge) => (
              <Edge
                key={edge.id}
                x1={edge.x1}
                y1={edge.y1}
                x2={edge.x2}
                y2={edge.y2}
                $related={edge.relation === "related"}
              />
            ))}
          </EdgeLayer>
          {graph.nodes.map((node: VaultGraphNode) => {
            const selected: boolean = node.kind === "vault"
              ? selectedVaultId === node.vaultId && !selectedSourceId
              : selectedSourceId === node.sourceId;
            return (
              <GraphNode
                key={node.id}
                type="button"
                $kind={node.kind}
                $x={node.x}
                $y={node.y}
                $attached={node.attached}
                $selected={selected}
                aria-pressed={selected}
                aria-label={`${node.kind === "vault" ? "Vault" : "Source"}: ${node.label}. ${node.detail}${node.attached ? ". Included in Chat context" : ""}`}
                onClick={(): void => node.sourceId
                  ? onSelectSource(node.vaultId, node.sourceId)
                  : onSelectVault(node.vaultId)}
              >
                {node.attached
                  ? <Paperclip size={17} weight="fill" />
                  : node.kind === "vault"
                    ? <FolderOpen size={18} weight="duotone" />
                    : <FileText size={17} weight="duotone" />}
                <span><strong>{node.label}</strong><small>{node.detail}</small></span>
              </GraphNode>
            );
          })}
        </GraphCanvas>
      </GraphViewport>
    </GraphShell>
  );
}
