import { beforeEach, describe, expect, it, vi } from "vitest";
import type { StoredWorkspaceRecord, WorkspaceSnapshot } from "../types/workspaceSnapshotTypes";
import { inspectWorkspaceChanges } from "./workspaceCheckService";

const repository = vi.hoisted(() => ({
  getWorkspaceRecord: vi.fn(),
  savePendingWorkspaceSnapshot: vi.fn(),
  acceptPendingWorkspaceSnapshot: vi.fn()
}));
const snapshots = vi.hoisted(() => ({
  captureWorkspaceSnapshot: vi.fn(),
  compareWorkspaceSnapshots: vi.fn()
}));

vi.mock("../repositories/workspaceRepository", () => repository);
vi.mock("./workspaceSnapshotService", () => snapshots);

const ownerId = "0c0d3611-301c-4ff4-8656-30a3cf16edbd";
const workspaceId = "427d6713-f2d4-4b0d-8f72-eaa7f19ebd23";
const baseline: WorkspaceSnapshot = { capturedAt: "2026-08-20T00:00:00.000Z", entries: [], truncated: false };
const current: WorkspaceSnapshot = { capturedAt: "2026-08-20T01:00:00.000Z", entries: [], truncated: false };

function recordWithPermission(permission: "granted" | "prompt"): StoredWorkspaceRecord {
  const directoryHandle = {
    kind: "directory",
    name: "nexo-ia",
    queryPermission: vi.fn().mockResolvedValue(permission),
    requestPermission: vi.fn().mockResolvedValue("granted")
  } as unknown as FileSystemDirectoryHandle;
  return {
    key: `${ownerId}:${workspaceId}`,
    ownerId,
    workspace: {
      id: workspaceId,
      ownerId,
      name: "nexo-ia",
      directoryName: "nexo-ia",
      access: "read",
      platform: "linux",
      source: "local-directory",
      addedAt: "2026-08-20T00:00:00.000Z"
    },
    directoryHandle,
    snapshot: baseline,
    pendingSnapshot: null
  };
}

describe("workspaceCheckService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    repository.savePendingWorkspaceSnapshot.mockResolvedValue(undefined);
  });

  it("returns a visible change summary and preserves the previous baseline", async () => {
    const record: StoredWorkspaceRecord = recordWithPermission("granted");
    repository.getWorkspaceRecord.mockResolvedValue(record);
    snapshots.captureWorkspaceSnapshot.mockResolvedValue(current);
    snapshots.compareWorkspaceSnapshots.mockReturnValue({
      added: ["src/new.ts"],
      removed: ["src/old.ts"],
      modified: ["README.md"],
      truncated: false
    });

    const result = await inspectWorkspaceChanges(ownerId, workspaceId, false);

    expect(result.status).toBe("changed");
    expect(result.changes).toMatchObject({ added: 1, removed: 1, modified: 1 });
    expect(repository.savePendingWorkspaceSnapshot).toHaveBeenCalledWith(record, current);
  });

  it("requires a fresh user confirmation when browser permission is not persisted", async () => {
    repository.getWorkspaceRecord.mockResolvedValue(recordWithPermission("prompt"));

    const result = await inspectWorkspaceChanges(ownerId, workspaceId, false);

    expect(result.status).toBe("permission-required");
    expect(snapshots.captureWorkspaceSnapshot).not.toHaveBeenCalled();
  });
});
