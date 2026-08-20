import type { z } from "zod";
import type { addWorkspaceSchema } from "../schemas/addWorkspaceSchema";

export type WorkspaceAccess = "read" | "propose" | "commands";

export type ProjectWorkspace = {
  id: string;
  name: string;
  path: string;
  access: WorkspaceAccess;
  branch?: string;
  addedAt: string;
};

export type AddWorkspaceValues = z.infer<typeof addWorkspaceSchema>;

export type WorkspaceState = {
  workspaces: ProjectWorkspace[];
  activeWorkspaceId: string | null;
  addWorkspace: (values: AddWorkspaceValues) => ProjectWorkspace;
  selectWorkspace: (workspaceId: string | null) => void;
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
