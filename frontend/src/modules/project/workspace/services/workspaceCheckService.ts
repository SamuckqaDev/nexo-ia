import {
  acceptPendingWorkspaceSnapshot,
  getWorkspaceRecord,
  savePendingWorkspaceSnapshot
} from "../repositories/workspaceRepository";
import type { StoredWorkspaceRecord, WorkspaceSnapshotDifference } from "../types/workspaceSnapshotTypes";
import type { WorkspaceChangeSummary, WorkspaceCheck } from "../types/workspaceTypes";
import { captureWorkspaceSnapshot, compareWorkspaceSnapshots } from "./workspaceSnapshotService";

type PermissionState = "granted" | "denied" | "prompt";
type PermissionAwareDirectoryHandle = FileSystemDirectoryHandle & {
  queryPermission?: (descriptor: { mode: "read" }) => Promise<PermissionState>;
  requestPermission?: (descriptor: { mode: "read" }) => Promise<PermissionState>;
};

function checkResult(
  workspaceId: string,
  status: WorkspaceCheck["status"],
  message: string | null,
  changes: WorkspaceChangeSummary | null = null
): WorkspaceCheck {
  return {
    workspaceId,
    status,
    checkedAt: new Date().toISOString(),
    message,
    changes
  };
}

function permissionRequired(workspaceId: string): WorkspaceCheck {
  return checkResult(
    workspaceId,
    "permission-required",
    "Confirm folder access so Nexo can check whether this project changed."
  );
}

function changeSummary(difference: WorkspaceSnapshotDifference): WorkspaceChangeSummary {
  return {
    added: difference.added.length,
    removed: difference.removed.length,
    modified: difference.modified.length,
    samples: [...difference.added, ...difference.removed, ...difference.modified].slice(0, 5),
    truncated: difference.truncated
  };
}

function safeErrorMessage(error: unknown): string {
  if (error instanceof DOMException && (error.name === "NotAllowedError" || error.name === "SecurityError")) {
    return "Folder access must be confirmed before Nexo can inspect project changes.";
  }
  return "Nexo could not inspect this workspace. The folder may have moved or become unavailable.";
}

async function hasReadPermission(
  handle: PermissionAwareDirectoryHandle,
  requestPermission: boolean
): Promise<boolean> {
  if (!handle.queryPermission) return true;
  const current: PermissionState = await handle.queryPermission({ mode: "read" });
  if (current === "granted") return true;
  if (!requestPermission || !handle.requestPermission) return false;
  return (await handle.requestPermission({ mode: "read" })) === "granted";
}

export async function inspectWorkspaceChanges(
  ownerId: string,
  workspaceId: string,
  requestPermission: boolean
): Promise<WorkspaceCheck> {
  const record: StoredWorkspaceRecord | null = await getWorkspaceRecord(ownerId, workspaceId);
  if (!record) {
    return checkResult(workspaceId, "missing", "The saved folder handle is no longer available on this device.");
  }

  const handle: PermissionAwareDirectoryHandle = record.directoryHandle;
  if (!(await hasReadPermission(handle, requestPermission))) return permissionRequired(workspaceId);

  try {
    const currentSnapshot = await captureWorkspaceSnapshot(record.directoryHandle);
    const difference: WorkspaceSnapshotDifference = compareWorkspaceSnapshots(record.snapshot, currentSnapshot);
    const changed: boolean = difference.added.length > 0 || difference.removed.length > 0 || difference.modified.length > 0;

    if (changed) {
      await savePendingWorkspaceSnapshot(record, currentSnapshot);
      return checkResult(
        workspaceId,
        "changed",
        "The project structure changed since Nexo last accepted this workspace snapshot.",
        changeSummary(difference)
      );
    }

    await savePendingWorkspaceSnapshot(record, null, currentSnapshot);
    return checkResult(workspaceId, "unchanged", null);
  } catch (error: unknown) {
    if (error instanceof DOMException && (error.name === "NotAllowedError" || error.name === "SecurityError")) {
      return permissionRequired(workspaceId);
    }
    return checkResult(workspaceId, "error", safeErrorMessage(error));
  }
}

export function acceptWorkspaceChanges(ownerId: string, workspaceId: string): Promise<void> {
  return acceptPendingWorkspaceSnapshot(ownerId, workspaceId);
}
