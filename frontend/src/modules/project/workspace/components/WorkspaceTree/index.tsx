import { CaretRight, File, Folder, FolderOpen, MagnifyingGlass, WarningCircle } from "@phosphor-icons/react";
import { useEffect, useMemo, useState, type ReactElement } from "react";
import type {
  WorkspaceSnapshotEntry,
  WorkspaceSnapshotOmission,
  WorkspaceSnapshotOmissionReason,
  WorkspaceTreeNode,
  WorkspaceTreeProps
} from "../../types/workspaceSnapshotTypes";
import {
  EmptyFilter,
  Meta,
  MetaCopy,
  Node,
  NodeButton,
  OmissionList,
  ScanNotice,
  Search,
  ToolButton,
  Tree,
  TreeFrame,
  TreeTools
} from "./styles";

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

function directoryPaths(nodes: WorkspaceTreeNode[]): string[] {
  return nodes.flatMap((node: WorkspaceTreeNode): string[] => node.kind === "directory"
    ? [node.path, ...directoryPaths(node.children)]
    : []);
}

function filteredTree(nodes: WorkspaceTreeNode[], query: string): WorkspaceTreeNode[] {
  if (!query) return nodes;
  return nodes.flatMap((node: WorkspaceTreeNode): WorkspaceTreeNode[] => {
    const children: WorkspaceTreeNode[] = filteredTree(node.children, query);
    return node.path.toLocaleLowerCase().includes(query) || children.length
      ? [{ ...node, children }]
      : [];
  });
}

function omissionLabel(reason: WorkspaceSnapshotOmissionReason): string {
  if (reason === "ignored-directory") return "Generated or dependency directory skipped";
  if (reason === "entry-limit") return "Entry limit reached";
  if (reason === "depth-limit") return "Depth limit reached";
  return "Browser could not read this entry";
}

export function WorkspaceTree({ snapshot, compact = false }: WorkspaceTreeProps): ReactElement {
  const roots: WorkspaceTreeNode[] = useMemo<WorkspaceTreeNode[]>(() => workspaceTree(snapshot.entries), [snapshot.entries]);
  const paths: string[] = useMemo<string[]>(() => directoryPaths(roots), [roots]);
  const rootDirectories: string[] = useMemo<string[]>(() => roots
    .filter((node: WorkspaceTreeNode): boolean => node.kind === "directory")
    .map((node: WorkspaceTreeNode): string => node.path), [roots]);
  const [expanded, setExpanded] = useState<Set<string>>(() => new Set(rootDirectories));
  const [query, setQuery] = useState<string>("");
  const directoryCount: number = snapshot.entries.filter((entry: WorkspaceSnapshotEntry): boolean => entry.kind === "directory").length;
  const fileCount: number = snapshot.entries.length - directoryCount;
  const normalizedQuery: string = query.trim().toLocaleLowerCase();
  const visibleRoots: WorkspaceTreeNode[] = useMemo<WorkspaceTreeNode[]>(
    () => filteredTree(roots, normalizedQuery),
    [normalizedQuery, roots]
  );
  const omissions: WorkspaceSnapshotOmission[] = snapshot.scan?.omissions ?? [];
  const omittedPaths: Map<string, WorkspaceSnapshotOmissionReason> = useMemo(
    () => new Map(omissions.map((omission: WorkspaceSnapshotOmission) => [omission.path, omission.reason])),
    [omissions]
  );
  const allExpanded: boolean = paths.length > 0 && paths.every((path: string): boolean => expanded.has(path));

  useEffect((): void => {
    setExpanded(new Set(rootDirectories));
    setQuery("");
  }, [rootDirectories, snapshot.capturedAt]);

  const toggle = (path: string): void => setExpanded((current: Set<string>): Set<string> => {
    const next: Set<string> = new Set(current);
    if (next.has(path)) next.delete(path);
    else next.add(path);
    return next;
  });

  const renderNodes = (nodes: WorkspaceTreeNode[], depth: number): ReactElement => (
    <Tree role={depth === 0 ? "tree" : "group"}>
      {nodes.map((node: WorkspaceTreeNode) => {
        const isOpen: boolean = Boolean(normalizedQuery) || expanded.has(node.path);
        const size: string | null = formatBytes(node.size);
        const omissionReason: WorkspaceSnapshotOmissionReason | undefined = omittedPaths.get(node.path);
        return (
          <Node key={node.path} role="treeitem" aria-expanded={node.kind === "directory" ? isOpen : undefined}>
            {node.kind === "directory" ? (
              <NodeButton type="button" $depth={depth} onClick={(): void => toggle(node.path)}>
                <CaretRight size={12} weight="bold" className={isOpen ? "open" : ""} />
                {isOpen ? <FolderOpen size={16} weight="duotone" /> : <Folder size={16} weight="duotone" />}
                <span>{node.name}</span>
                <small>{omissionReason ? "not scanned" : node.children.length}</small>
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
        <MetaCopy>
          <span>{directoryCount} folders · {fileCount} files</span>
          <small>{snapshot.truncated ? "Partial snapshot" : `Captured ${new Date(snapshot.capturedAt).toLocaleString()}`}</small>
        </MetaCopy>
        {!compact && paths.length > 0 && (
          <TreeTools>
            <ToolButton
              type="button"
              onClick={(): void => setExpanded(allExpanded ? new Set() : new Set(paths))}
            >
              {allExpanded ? "Collapse all" : "Expand all"}
            </ToolButton>
          </TreeTools>
        )}
      </Meta>
      {!compact && roots.length > 0 && (
        <Search>
          <MagnifyingGlass size={15} />
          <input
            type="search"
            aria-label="Filter workspace structure"
            placeholder="Find a folder or file by path"
            value={query}
            onChange={(event): void => setQuery(event.target.value)}
          />
        </Search>
      )}
      {snapshot.scan && snapshot.scan.omissionCount > 0 && (
        <ScanNotice>
          <summary><WarningCircle size={15} /> {snapshot.scan.omissionCount} path{snapshot.scan.omissionCount === 1 ? "" : "s"} not fully scanned</summary>
          <p>Captured up to {snapshot.scan.maxEntries.toLocaleString()} entries and {snapshot.scan.maxDepth} nested levels. These paths stay visible when their directory entry was readable.</p>
          <OmissionList>
            {omissions.map((omission: WorkspaceSnapshotOmission) => (
              <li key={`${omission.reason}:${omission.path}`}><code>{omission.path}</code><span>{omissionLabel(omission.reason)}</span></li>
            ))}
          </OmissionList>
          {snapshot.scan.omissionCount > omissions.length && <small>Only the first {omissions.length} omitted paths are shown.</small>}
        </ScanNotice>
      )}
      {visibleRoots.length
        ? renderNodes(visibleRoots, 0)
        : roots.length
          ? <EmptyFilter>No paths match “{query}”.</EmptyFilter>
          : <EmptyFilter>This workspace folder is empty.</EmptyFilter>}
    </TreeFrame>
  );
}
