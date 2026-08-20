import type { z } from "zod";
import type { addWorkspaceSchema } from "../schemas/addWorkspaceSchema";
import type { projectWorkspaceSchema } from "../schemas/projectWorkspaceSchema";

export type WorkspaceAccess = "read" | "propose" | "commands";
export type WorkspacePlatform = "windows" | "macos" | "linux" | "unknown";
export type WorkspaceHydrationStatus = "idle" | "loading" | "ready" | "error";
export type WorkspaceCheckStatus =
  | "idle"
  | "checking"
  | "unchanged"
  | "changed"
  | "permission-required"
  | "missing"
  | "unsupported"
  | "error";

export type ProjectWorkspace = z.infer<typeof projectWorkspaceSchema>;
export type AddWorkspaceValues = z.infer<typeof addWorkspaceSchema>;

export type WorkspaceChangeSummary = {
  added: number;
  removed: number;
  modified: number;
  samples: string[];
  truncated: boolean;
};

export type WorkspaceCheck = {
  workspaceId: string | null;
  status: WorkspaceCheckStatus;
  checkedAt: string | null;
  message: string | null;
  changes: WorkspaceChangeSummary | null;
};

export type WorkspaceState = {
  ownerId: string | null;
  workspaces: ProjectWorkspace[];
  activeWorkspaceId: string | null;
  hydrationStatus: WorkspaceHydrationStatus;
  persistenceError: string | null;
  workspaceCheck: WorkspaceCheck;
  initialize: (ownerId: string) => Promise<void>;
  registerWorkspace: (workspace: ProjectWorkspace) => void;
  selectWorkspace: (workspaceId: string | null) => void;
  forgetWorkspace: (workspaceId: string) => Promise<void>;
  setWorkspaceCheck: (check: WorkspaceCheck) => void;
};

export type WorkspaceSwitcherProps = {
  collapsed: boolean;
  onManage: () => void;
};

export type WorkspaceFormProps = {
  onAdded: (workspace: ProjectWorkspace) => void;
  onCancel: () => void;
};

export type ProjectsPageProps = { onOpenChat: () => void };
