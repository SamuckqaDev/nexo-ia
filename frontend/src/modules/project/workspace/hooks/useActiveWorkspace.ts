import { useEffect } from "react";
import { useServerWorkspaceSelectionStore } from "../stores/useServerWorkspaceSelectionStore";
import type { ServerWorkspaceSelectionState } from "../stores/useServerWorkspaceSelectionStore";
import type { ServerWorkspace } from "../types/serverWorkspaceTypes";
import { useServerWorkspaces } from "./useServerWorkspaces";

export function useActiveWorkspace(): ServerWorkspace | undefined {
  const workspaces = useServerWorkspaces();
  const selectedWorkspaceId: string | null = useServerWorkspaceSelectionStore(
    (state: ServerWorkspaceSelectionState) => state.selectedWorkspaceId);
  const selectWorkspace: ServerWorkspaceSelectionState["selectWorkspace"] =
    useServerWorkspaceSelectionStore((state: ServerWorkspaceSelectionState) => state.selectWorkspace);
  const selected: ServerWorkspace | undefined = workspaces.data?.find(
    (workspace: ServerWorkspace): boolean => workspace.id === selectedWorkspaceId)
    ?? workspaces.data?.[0];

  useEffect((): void => {
    if (selected && selected.id !== selectedWorkspaceId) selectWorkspace(selected.id);
  }, [selectWorkspace, selected, selectedWorkspaceId]);

  return selected;
}
