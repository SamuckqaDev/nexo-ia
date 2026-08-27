import { randomUUID } from "node:crypto";
import { basename } from "node:path";
import type { DesktopWorkspaceSelection } from "../protocol/types.js";

const DEFAULT_TTL_MS = 5 * 60 * 1_000;

type PendingSelection = {
  rootPath: string;
  expiresAt: number;
};

type WorkspaceSelectionStoreOptions = {
  clock?: () => number;
  idFactory?: () => string;
  ttlMs?: number;
};

/** Keeps native paths inside Electron while the authenticated server registration is created. */
export class WorkspaceSelectionStore {
  private readonly pending = new Map<string, PendingSelection>();
  private readonly clock: () => number;
  private readonly idFactory: () => string;
  private readonly ttlMs: number;

  constructor(options: WorkspaceSelectionStoreOptions = {}) {
    this.clock = options.clock ?? Date.now;
    this.idFactory = options.idFactory ?? randomUUID;
    this.ttlMs = options.ttlMs ?? DEFAULT_TTL_MS;
  }

  create(rootPath: string): DesktopWorkspaceSelection {
    const normalized = rootPath.trim();
    if (!normalized) throw new Error("The selected workspace folder is invalid");
    this.prune();
    const selectionId = this.idFactory();
    this.pending.set(selectionId, { rootPath: normalized, expiresAt: this.clock() + this.ttlMs });
    return { selectionId, displayName: basename(normalized) || "Local workspace" };
  }

  consume(selectionId: string): string {
    this.prune();
    const selection = this.pending.get(selectionId);
    if (!selection) throw new Error("The folder selection expired. Choose the workspace folder again");
    this.pending.delete(selectionId);
    return selection.rootPath;
  }

  private prune(): void {
    const now = this.clock();
    for (const [selectionId, selection] of this.pending) {
      if (selection.expiresAt <= now) this.pending.delete(selectionId);
    }
  }
}
