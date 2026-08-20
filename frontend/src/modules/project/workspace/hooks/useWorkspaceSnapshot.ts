import { useEffect, useState } from "react";
import { getWorkspaceSnapshot } from "../repositories/workspaceRepository";
import { useWorkspaceStore } from "../stores/useWorkspaceStore";
import type { WorkspaceSnapshotResult } from "../types/workspaceHookTypes";
import type { WorkspaceSnapshot } from "../types/workspaceSnapshotTypes";
import type { WorkspaceState } from "../types/workspaceTypes";

export function useWorkspaceSnapshot(workspaceId: string | null): WorkspaceSnapshotResult {
  const ownerId: string | null = useWorkspaceStore((state: WorkspaceState) => state.ownerId);
  const checkedAt: string | null = useWorkspaceStore((state: WorkspaceState) => state.workspaceCheck.checkedAt);
  const [result, setResult] = useState<WorkspaceSnapshotResult>({ snapshot: null, status: "idle" });

  useEffect((): (() => void) | void => {
    if (!ownerId || !workspaceId) {
      setResult({ snapshot: null, status: "idle" });
      return;
    }

    let active: boolean = true;
    setResult((current: WorkspaceSnapshotResult): WorkspaceSnapshotResult => ({ ...current, status: "loading" }));
    getWorkspaceSnapshot(ownerId, workspaceId)
      .then((snapshot: WorkspaceSnapshot | null): void => {
        if (active) setResult({ snapshot, status: "ready" });
      })
      .catch((): void => {
        if (active) setResult({ snapshot: null, status: "error" });
      });

    return (): void => { active = false; };
  }, [checkedAt, ownerId, workspaceId]);

  return result;
}
