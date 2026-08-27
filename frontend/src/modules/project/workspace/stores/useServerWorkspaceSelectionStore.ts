import { create } from "zustand";

const STORAGE_PREFIX = "nexo.server-workspace-selection";

type ServerWorkspaceSelectionState = {
  ownerId: string | null;
  selectedWorkspaceId: string | null;
  initialize: (ownerId: string) => void;
  selectWorkspace: (workspaceId: string | null) => void;
};

const storageKey = (ownerId: string): string => `${STORAGE_PREFIX}:${ownerId}`;

const storedSelection = (ownerId: string): string | null => {
  if (typeof window === "undefined") return null;
  try {
    return window.localStorage.getItem(storageKey(ownerId)) || null;
  } catch {
    return null;
  }
};

const persistSelection = (ownerId: string, workspaceId: string | null): void => {
  if (typeof window === "undefined") return;
  try {
    if (workspaceId) window.localStorage.setItem(storageKey(ownerId), workspaceId);
    else window.localStorage.removeItem(storageKey(ownerId));
  } catch {
    // Browser storage is an optional convenience; the authenticated server remains authoritative.
  }
};

/** Keeps one UI-level project choice consistent across Projects, Home, Chat, Cowork and Skills. */
export const useServerWorkspaceSelectionStore = create<ServerWorkspaceSelectionState>((set, get) => ({
  ownerId: null,
  selectedWorkspaceId: null,
  initialize: (ownerId: string): void => {
    if (get().ownerId === ownerId) return;
    set({ ownerId, selectedWorkspaceId: storedSelection(ownerId) });
  },
  selectWorkspace: (workspaceId: string | null): void => {
    const ownerId: string | null = get().ownerId;
    set({ selectedWorkspaceId: workspaceId });
    if (ownerId) persistSelection(ownerId, workspaceId);
  }
}));

export type { ServerWorkspaceSelectionState };
