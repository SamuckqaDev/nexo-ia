import { CaretRight, File, Folder, FolderOpen } from "@phosphor-icons/react";
import { useMemo, useState, type ReactElement } from "react";
import type {
  WorkspaceSnapshotEntry,
  WorkspaceTreeNode,
  WorkspaceTreeProps
} from "../../types/workspaceSnapshotTypes";
import { Meta, Node, NodeButton, Tree, TreeFrame } from "./styles";

function workspaceTree(entries: WorkspaceSnapshotEntry[]): WorkspaceTreeNode[] {
  const roots: WorkspaceTreeNode[] = [];
  const nodes: Map<string, WorkspaceTreeNode> = new Map();

  entries.forEach((entry: WorkspaceSnapshotEntry): void => {
    const segments: string[] = entry.path.split("/").filter(Boolean);
    segments.forEach((name: string, index: number): void => {
      const path: string = segments.slice(0, index + 1).join("/");
      if (nodes.has(path)) return;
      const isEntry: boolean = index === segments.length - 1;
      const node: WorkspaceTreeNode = {
        path,
        name,
        kind: isEntry ? entry.kind : "directory",
        size: isEntry ? entry.size : null,
        children: []
      };
      nodes.set(path, node);
      const parentPath: string = segments.slice(0, index).join("/");
      const parent: WorkspaceTreeNode | undefined = nodes.get(parentPath);
      if (parent) parent.children.push(node);
      else roots.push(node);
    });
  });

  const sortNodes = (items: WorkspaceTreeNode[]): void => {
    items.sort((left: WorkspaceTreeNode, right: WorkspaceTreeNode): number =>
      left.kind === right.kind ? left.name.localeCompare(right.name) : left.kind === "directory" ? -1 : 1);
    items.forEach((item: WorkspaceTreeNode): void => sortNodes(item.children));
  };
  sortNodes(roots);
  return roots;
}

function formatBytes(size: number | null): string | null {
  if (size === null) return null;
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${Math.round(size / 1024)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

export function WorkspaceTree({ snapshot, compact = false }: WorkspaceTreeProps): ReactElement {
  const roots: WorkspaceTreeNode[] = useMemo<WorkspaceTreeNode[]>(() => workspaceTree(snapshot.entries), [snapshot.entries]);
  const [expanded, setExpanded] = useState<Set<string>>(new Set());
  const directoryCount: number = snapshot.entries.filter((entry: WorkspaceSnapshotEntry): boolean => entry.kind === "directory").length;
  const fileCount: number = snapshot.entries.length - directoryCount;

  const toggle = (path: string): void => setExpanded((current: Set<string>): Set<string> => {
    const next: Set<string> = new Set(current);
    if (next.has(path)) next.delete(path);
    else next.add(path);
    return next;
  });

  const renderNodes = (nodes: WorkspaceTreeNode[], depth: number): ReactElement => (
    <Tree role={depth === 0 ? "tree" : "group"}>
      {nodes.map((node: WorkspaceTreeNode) => {
        const isOpen: boolean = expanded.has(node.path);
        const size: string | null = formatBytes(node.size);
        return (
          <Node key={node.path} role="treeitem" aria-expanded={node.kind === "directory" ? isOpen : undefined}>
            {node.kind === "directory" ? (
              <NodeButton type="button" $depth={depth} onClick={(): void => toggle(node.path)}>
                <CaretRight size={12} weight="bold" className={isOpen ? "open" : ""} />
                {isOpen ? <FolderOpen size={16} weight="duotone" /> : <Folder size={16} weight="duotone" />}
                <span>{node.name}</span>
                <small>{node.children.length}</small>
              </NodeButton>
            ) : (
              <NodeButton as="div" $depth={depth}>
                <i />
                <File size={15} weight="duotone" />
                <span>{node.name}</span>
                {size && <small>{size}</small>}
              </NodeButton>
            )}
            {node.kind === "directory" && isOpen && renderNodes(node.children, depth + 1)}
          </Node>
        );
      })}
    </Tree>
  );

  return (
    <TreeFrame $compact={compact}>
      <Meta>
        <span>{directoryCount} folders · {fileCount} files</span>
        <small>{snapshot.truncated ? "Bounded snapshot" : `Captured ${new Date(snapshot.capturedAt).toLocaleString()}`}</small>
      </Meta>
      {roots.length ? renderNodes(roots, 0) : <Meta><span>This workspace folder is empty.</span></Meta>}
    </TreeFrame>
  );
}
