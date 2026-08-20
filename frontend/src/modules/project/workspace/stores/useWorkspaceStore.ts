import { create } from "zustand";
import {
  deleteWorkspaceRecord,
  loadWorkspaceRegistry,
  saveActiveWorkspaceId
} from "../repositories/workspaceRepository";
import type { WorkspaceRegistry } from "../types/workspaceSnapshotTypes";
import type { ProjectWorkspace, WorkspaceCheck, WorkspaceState } from "../types/workspaceTypes";

export const idleWorkspaceCheck: WorkspaceCheck = {
  workspaceId: null,
  status: "idle",
  checkedAt: null,
  message: null,
  changes: null
};

function persistenceMessage(): string {
  return "Nexo could not update the saved workspace on this device.";
}

export const useWorkspaceStore = create<WorkspaceState>((set, get) => ({
  ownerId: null,
  workspaces: [],
  activeWorkspaceId: null,
  hydrationStatus: "idle",
  persistenceError: null,
  workspaceCheck: idleWorkspaceCheck,
  initialize: (ownerId: string): Promise<void> => {
    const current: WorkspaceState = get();
    if (current.ownerId === ownerId && (current.hydrationStatus === "loading" || current.hydrationStatus === "ready")) {
      return Promise.resolve();
    }

    set({
      ownerId,
      workspaces: [],
      activeWorkspaceId: null,
      hydrationStatus: "loading",
      persistenceError: null,
      workspaceCheck: idleWorkspaceCheck
    });

    return loadWorkspaceRegistry(ownerId)
      .then((registry: WorkspaceRegistry): void => {
        if (get().ownerId !== ownerId) return;
        set({ ...registry, hydrationStatus: "ready", persistenceError: null });
      })
      .catch((): void => {
        if (get().ownerId !== ownerId) return;
        set({ hydrationStatus: "error", persistenceError: persistenceMessage() });
      });
  },
  registerWorkspace: (workspace: ProjectWorkspace): void => set((state: WorkspaceState) => ({
    workspaces: [workspace, ...state.workspaces.filter((item: ProjectWorkspace): boolean => item.id !== workspace.id)],
    activeWorkspaceId: workspace.id,
    persistenceError: null,
    workspaceCheck: idleWorkspaceCheck
  })),
  selectWorkspace: (workspaceId: string | null): void => {
    const state: WorkspaceState = get();
    const activeWorkspaceId: string | null = workspaceId
      && state.workspaces.some((workspace: ProjectWorkspace): boolean => workspace.id === workspaceId)
      ? workspaceId
      : null;
    set({ activeWorkspaceId, workspaceCheck: idleWorkspaceCheck });
    if (state.ownerId) {
      saveActiveWorkspaceId(state.ownerId, activeWorkspaceId)
        .catch((): void => set({ persistenceError: persistenceMessage() }));
    }
  },
  forgetWorkspace: (workspaceId: string): Promise<void> => {
    const state: WorkspaceState = get();
    const ownerId: string | null = state.ownerId;
    if (!ownerId) return Promise.resolve();

    return deleteWorkspaceRecord(ownerId, workspaceId).then((): void => {
      const current: WorkspaceState = get();
      const workspaces: ProjectWorkspace[] = current.workspaces
        .filter((workspace: ProjectWorkspace): boolean => workspace.id !== workspaceId);
      const activeWorkspaceId: string | null = current.activeWorkspaceId === workspaceId
        ? workspaces[0]?.id ?? null
        : current.activeWorkspaceId;
      set({ workspaces, activeWorkspaceId, persistenceError: null, workspaceCheck: idleWorkspaceCheck });
      saveActiveWorkspaceId(ownerId, activeWorkspaceId)
        .catch((): void => set({ persistenceError: persistenceMessage() }));
    }).catch((error: unknown): Promise<never> => {
      set({ persistenceError: persistenceMessage() });
      return Promise.reject(error);
    });
  },
  setWorkspaceCheck: (workspaceCheck: WorkspaceCheck): void => set({ workspaceCheck })
}));
