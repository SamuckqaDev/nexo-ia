import { useWorkspaceStore } from "../stores/useWorkspaceStore";
import type { ProjectWorkspace, WorkspaceState } from "../types/workspaceTypes";

export function useActiveWorkspace(): ProjectWorkspace | undefined {
  return useWorkspaceStore((state: WorkspaceState) => state.workspaces.find(
    (workspace: ProjectWorkspace) => workspace.id === state.activeWorkspaceId
  ));
}
