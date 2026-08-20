import type { ProjectWorkspace } from "./workspaceTypes";

export type WorkspaceEntryKind = "directory" | "file";

export type WorkspaceSnapshotEntry = {
  path: string;
  kind: WorkspaceEntryKind;
  size: number | null;
  lastModified: number | null;
};

export type WorkspaceSnapshot = {
  capturedAt: string;
  entries: WorkspaceSnapshotEntry[];
  truncated: boolean;
};

export type StoredWorkspaceRecord = {
  key: string;
  ownerId: string;
  workspace: ProjectWorkspace;
  directoryHandle: FileSystemDirectoryHandle;
  snapshot: WorkspaceSnapshot;
  pendingSnapshot: WorkspaceSnapshot | null;
};

export type StoredWorkspaceSelection = {
  ownerId: string;
  activeWorkspaceId: string | null;
};

export type WorkspaceRegistry = {
  workspaces: ProjectWorkspace[];
  activeWorkspaceId: string | null;
};

export type WorkspaceSnapshotDifference = {
  added: string[];
  removed: string[];
  modified: string[];
  truncated: boolean;
};

export type WorkspaceTreeNode = {
  path: string;
  name: string;
  kind: WorkspaceEntryKind;
  size: number | null;
  children: WorkspaceTreeNode[];
};

export type WorkspaceTreeProps = {
  snapshot: WorkspaceSnapshot;
  compact?: boolean;
};
