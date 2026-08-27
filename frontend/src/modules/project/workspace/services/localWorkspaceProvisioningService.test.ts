import { describe, expect, it, vi } from "vitest";
import type { DesktopRuntimeHook } from "../../../device/runtime/hooks/useDesktopRuntime";
import type { ServerWorkspace } from "../types/serverWorkspaceTypes";
import { provisionLocalWorkspace } from "./localWorkspaceProvisioningService";

const workspace: ServerWorkspace = {
  id: "427d6713-f2d4-4b0d-8f72-eaa7f19ebd23",
  name: "nexo-ia",
  storageType: "UNBOUND",
  accessMode: "READ_ONLY",
  status: "UNBOUND",
  relativePath: null,
  lastScannedAt: null,
  createdAt: "2026-08-27T00:00:00Z",
  updatedAt: "2026-08-27T00:00:00Z"
};

const runtime = (overrides: Partial<DesktopRuntimeHook> = {}): DesktopRuntimeHook => ({
  available: true,
  state: { paired: true, connected: true, deviceId: "device", serverUrl: "http://nexo", workspaces: [] },
  pending: false,
  error: null,
  pair: vi.fn(),
  selectWorkspaceDirectory: vi.fn().mockResolvedValue({
    selectionId: "selection-1",
    displayName: "nexo-ia",
    existingWorkspaceId: null
  }),
  chooseWorkspace: vi.fn().mockResolvedValue({ paired: true, connected: true, deviceId: "device", serverUrl: "http://nexo", workspaces: [] }),
  ...overrides
});

describe("provisionLocalWorkspace", () => {
  it("creates and binds a workspace after the native chooser returns", async () => {
    const desktop = runtime();
    const createWorkspace = vi.fn().mockResolvedValue(workspace);
    const deleteWorkspace = vi.fn();
    const listWorkspaces = vi.fn().mockResolvedValue([]);

    await expect(provisionLocalWorkspace(desktop, { createWorkspace, deleteWorkspace, listWorkspaces }))
      .resolves.toEqual(workspace);
    expect(createWorkspace).toHaveBeenCalledWith({
      name: "nexo-ia",
      storageType: "UNBOUND",
      accessMode: "READ_ONLY"
    });
    expect(desktop.chooseWorkspace).toHaveBeenCalledWith(workspace.id, workspace.name, "selection-1");
    expect(deleteWorkspace).not.toHaveBeenCalled();
  });

  it("does not create anything when the native chooser is cancelled", async () => {
    const desktop = runtime({ selectWorkspaceDirectory: vi.fn().mockResolvedValue(null) });
    const createWorkspace = vi.fn();
    const deleteWorkspace = vi.fn();
    const listWorkspaces = vi.fn();

    await expect(provisionLocalWorkspace(desktop, { createWorkspace, deleteWorkspace, listWorkspaces })).resolves.toBeNull();
    expect(createWorkspace).not.toHaveBeenCalled();
  });

  it("reuses the existing registration when the same native folder is selected again", async () => {
    const desktop = runtime({
      selectWorkspaceDirectory: vi.fn().mockResolvedValue({
        selectionId: "selection-2",
        displayName: "nexo-ia",
        existingWorkspaceId: workspace.id
      })
    });
    const createWorkspace = vi.fn();
    const deleteWorkspace = vi.fn();
    const listWorkspaces = vi.fn().mockResolvedValue([workspace]);

    await expect(provisionLocalWorkspace(desktop, { createWorkspace, deleteWorkspace, listWorkspaces }))
      .resolves.toEqual(workspace);
    expect(createWorkspace).not.toHaveBeenCalled();
    expect(desktop.chooseWorkspace).not.toHaveBeenCalled();
  });

  it("removes an empty registration when the desktop binding fails", async () => {
    const desktop = runtime({ chooseWorkspace: vi.fn().mockRejectedValue(new Error("binding failed")) });
    const createWorkspace = vi.fn().mockResolvedValue(workspace);
    const deleteWorkspace = vi.fn().mockResolvedValue(undefined);
    const listWorkspaces = vi.fn().mockResolvedValue([]);

    await expect(provisionLocalWorkspace(desktop, { createWorkspace, deleteWorkspace, listWorkspaces }))
      .rejects.toThrow("binding failed");
    expect(deleteWorkspace).toHaveBeenCalledWith(workspace.id);
  });
});
