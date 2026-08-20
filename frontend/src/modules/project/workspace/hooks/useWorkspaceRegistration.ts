import { useState } from "react";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import {
  listWorkspaceRecords,
  saveActiveWorkspaceId,
  saveWorkspaceRecord
} from "../repositories/workspaceRepository";
import {
  chooseLocalWorkspaceDirectory,
  isPickerCancellation,
  supportsLocalDirectoryPicker
} from "../services/workspaceDirectoryService";
import {
  detectWorkspacePlatform,
  workspacePickerLabel
} from "../services/workspacePlatformService";
import { captureWorkspaceSnapshot } from "../services/workspaceSnapshotService";
import { useWorkspaceStore } from "../stores/useWorkspaceStore";
import type { StoredWorkspaceRecord, WorkspaceSnapshot } from "../types/workspaceSnapshotTypes";
import type { WorkspaceRegistrationResult } from "../types/workspaceHookTypes";
import type { ProjectWorkspace, WorkspaceAccess, WorkspacePlatform, WorkspaceState } from "../types/workspaceTypes";

async function matchingWorkspace(
  records: StoredWorkspaceRecord[],
  directoryHandle: FileSystemDirectoryHandle
): Promise<StoredWorkspaceRecord | null> {
  for (const record of records) {
    try {
      if (await record.directoryHandle.isSameEntry(directoryHandle)) return record;
    } catch {
      // A stale handle must not prevent the user from selecting another valid folder.
    }
  }
  return null;
}

function selectionError(error: unknown): string {
  if (error instanceof Error && error.message) return error.message;
  return "Nexo could not save this folder as a workspace.";
}

export function useWorkspaceRegistration(): WorkspaceRegistrationResult {
  const [isPicking, setIsPicking] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const platform: WorkspacePlatform = detectWorkspacePlatform();
  const registerWorkspace: WorkspaceState["registerWorkspace"] = useWorkspaceStore((state: WorkspaceState) => state.registerWorkspace);
  const show: SnackbarState["show"] = useSnackbarStore((state: SnackbarState) => state.show);

  const chooseFolder = (access: WorkspaceAccess): Promise<ProjectWorkspace | null> => {
    const ownerId: string | null = useWorkspaceStore.getState().ownerId;
    if (!ownerId) {
      const message = "Your signed-in workspace profile is still loading.";
      setError(message);
      return Promise.resolve(null);
    }

    setIsPicking(true);
    setError(null);
    return chooseLocalWorkspaceDirectory()
      .then((directoryHandle: FileSystemDirectoryHandle): Promise<ProjectWorkspace> =>
        listWorkspaceRecords(ownerId).then((records: StoredWorkspaceRecord[]): Promise<ProjectWorkspace> =>
          matchingWorkspace(records, directoryHandle).then((existing: StoredWorkspaceRecord | null): Promise<ProjectWorkspace> => {
            if (existing) {
              return saveActiveWorkspaceId(ownerId, existing.workspace.id).then((): ProjectWorkspace => existing.workspace);
            }

            return captureWorkspaceSnapshot(directoryHandle).then((snapshot: WorkspaceSnapshot): Promise<ProjectWorkspace> => {
              const workspace: ProjectWorkspace = {
                id: crypto.randomUUID(),
                ownerId,
                name: directoryHandle.name,
                directoryName: directoryHandle.name,
                access,
                platform,
                source: "local-directory",
                addedAt: new Date().toISOString()
              };
              const record: StoredWorkspaceRecord = {
                key: `${ownerId}:${workspace.id}`,
                ownerId,
                workspace,
                directoryHandle,
                snapshot,
                pendingSnapshot: null
              };
              return Promise.all([saveWorkspaceRecord(record), saveActiveWorkspaceId(ownerId, workspace.id)])
                .then((): ProjectWorkspace => workspace);
            });
          })
        )
      )
      .then((workspace: ProjectWorkspace): ProjectWorkspace => {
        registerWorkspace(workspace);
        show(`${workspace.name} is now the active workspace.`, { variant: "success" });
        return workspace;
      })
      .catch((selectionFailure: unknown): null => {
        if (isPickerCancellation(selectionFailure)) return null;
        const message: string = selectionError(selectionFailure);
        setError(message);
        show(message, { variant: "error" });
        return null;
      })
      .finally((): void => setIsPicking(false));
  };

  return {
    isSupported: supportsLocalDirectoryPicker(),
    isPicking,
    platform,
    actionLabel: workspacePickerLabel(platform),
    error,
    chooseFolder
  };
}
