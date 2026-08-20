import { create } from "zustand";
import type { AddWorkspaceValues, ProjectWorkspace, WorkspaceState } from "../types/workspaceTypes";

export const useWorkspaceStore = create<WorkspaceState>((set) => ({
  workspaces: [],
  activeWorkspaceId: null,
  addWorkspace: (values: AddWorkspaceValues): ProjectWorkspace => {
    const workspace: ProjectWorkspace = {
      id: crypto.randomUUID(),
      name: values.name,
      path: values.path,
      access: values.access,
      addedAt: new Date().toISOString()
    };
    set((state: WorkspaceState) => ({ workspaces: [workspace, ...state.workspaces], activeWorkspaceId: workspace.id }));
    return workspace;
  },
  selectWorkspace: (workspaceId: string | null): void => set((state: WorkspaceState) => ({
    activeWorkspaceId: workspaceId && state.workspaces.some((workspace: ProjectWorkspace) => workspace.id === workspaceId)
      ? workspaceId
      : null
  }))
}));
