import { acceptWorkspaceChanges, inspectWorkspaceChanges } from "../services/workspaceCheckService";
import { idleWorkspaceCheck, useWorkspaceStore } from "../stores/useWorkspaceStore";
import type { WorkspaceCheckResult } from "../types/workspaceHookTypes";
import type { WorkspaceCheck, WorkspaceState } from "../types/workspaceTypes";

export function useWorkspaceCheck(): WorkspaceCheckResult {
  const setWorkspaceCheck: WorkspaceState["setWorkspaceCheck"] = useWorkspaceStore((state: WorkspaceState) => state.setWorkspaceCheck);

  const checkActiveWorkspace = (requestPermission: boolean): Promise<WorkspaceCheck> => {
    const state: WorkspaceState = useWorkspaceStore.getState();
    if (!state.ownerId || !state.activeWorkspaceId) {
      setWorkspaceCheck(idleWorkspaceCheck);
      return Promise.resolve(idleWorkspaceCheck);
    }

    const checking: WorkspaceCheck = {
      workspaceId: state.activeWorkspaceId,
      status: "checking",
      checkedAt: null,
      message: null,
      changes: null
    };
    setWorkspaceCheck(checking);

    return inspectWorkspaceChanges(state.ownerId, state.activeWorkspaceId, requestPermission)
      .then((result: WorkspaceCheck): WorkspaceCheck => {
        setWorkspaceCheck(result);
        return result;
      })
      .catch((): WorkspaceCheck => {
        const failed: WorkspaceCheck = {
          workspaceId: state.activeWorkspaceId,
          status: "error",
          checkedAt: new Date().toISOString(),
          message: "Nexo could not read the saved workspace state from this browser.",
          changes: null
        };
        setWorkspaceCheck(failed);
        return failed;
      });
  };

  const acceptCurrentStructure = (): Promise<void> => {
    const state: WorkspaceState = useWorkspaceStore.getState();
    const workspaceId: string | null = state.workspaceCheck.workspaceId;
    if (!state.ownerId || !workspaceId) return Promise.resolve();

    return acceptWorkspaceChanges(state.ownerId, workspaceId)
      .then((): void => {
        setWorkspaceCheck({
          workspaceId,
          status: "unchanged",
          checkedAt: new Date().toISOString(),
          message: null,
          changes: null
        });
      })
      .catch((): void => {
        setWorkspaceCheck({
          workspaceId,
          status: "error",
          checkedAt: new Date().toISOString(),
          message: "Nexo could not save the updated workspace snapshot.",
          changes: null
        });
      });
  };

  return { checkActiveWorkspace, acceptCurrentStructure };
}
