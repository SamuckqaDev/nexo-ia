import { describe, expect, it } from "vitest";
import type { ProjectWorkspace } from "../types/workspaceTypes";
import { useWorkspaceStore } from "./useWorkspaceStore";

const workspace: ProjectWorkspace = {
  id: "427d6713-f2d4-4b0d-8f72-eaa7f19ebd23",
  ownerId: "0c0d3611-301c-4ff4-8656-30a3cf16edbd",
  name: "nexo-ia",
  directoryName: "nexo-ia",
  access: "read",
  platform: "macos",
  source: "local-directory",
  addedAt: "2026-08-20T00:00:00Z"
};

describe("useWorkspaceStore", () => {
  it("does not report a freshly captured workspace as changed before its first Chat", () => {
    const capturedAt = "2026-08-20T12:00:00Z";
    useWorkspaceStore.getState().registerWorkspace(workspace, capturedAt);

    expect(useWorkspaceStore.getState().workspaceCheck).toMatchObject({
      workspaceId: workspace.id,
      status: "unchanged",
      checkedAt: capturedAt
    });

    useWorkspaceStore.getState().selectWorkspace(workspace.id);
    expect(useWorkspaceStore.getState().workspaceCheck.status).toBe("unchanged");
    expect(useWorkspaceStore.getState().skipNextWorkspaceCheck).toBe(true);

    useWorkspaceStore.getState().consumeWorkspaceCheckSkip();
    expect(useWorkspaceStore.getState().skipNextWorkspaceCheck).toBe(false);
  });
});
