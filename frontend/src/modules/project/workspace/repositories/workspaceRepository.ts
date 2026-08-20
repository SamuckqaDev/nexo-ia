import { storedWorkspaceRecordSchema, storedWorkspaceSelectionSchema } from "../schemas/workspaceSnapshotSchema";
import type {
  StoredWorkspaceRecord,
  StoredWorkspaceSelection,
  WorkspaceRegistry,
  WorkspaceSnapshot
} from "../types/workspaceSnapshotTypes";
import type { ProjectWorkspace } from "../types/workspaceTypes";

const DATABASE_NAME = "nexo-local-workspaces";
const DATABASE_VERSION = 1;
const WORKSPACE_STORE = "workspaces";
const SELECTION_STORE = "selections";

function workspaceKey(ownerId: string, workspaceId: string): string {
  return `${ownerId}:${workspaceId}`;
}

function requestResult<T>(request: IDBRequest<T>): Promise<T> {
  return new Promise<T>((resolve, reject): void => {
    request.onsuccess = (): void => resolve(request.result);
    request.onerror = (): void => reject(request.error ?? new Error("The browser could not read local workspace storage."));
  });
}

function transactionComplete(transaction: IDBTransaction): Promise<void> {
  return new Promise<void>((resolve, reject): void => {
    transaction.oncomplete = (): void => resolve();
    transaction.onerror = (): void => reject(transaction.error ?? new Error("The browser could not save the workspace."));
    transaction.onabort = (): void => reject(transaction.error ?? new Error("The browser cancelled the workspace update."));
  });
}

function openWorkspaceDatabase(): Promise<IDBDatabase> {
  if (!("indexedDB" in window)) {
    return Promise.reject(new Error("This browser does not provide local workspace storage."));
  }

  return new Promise<IDBDatabase>((resolve, reject): void => {
    const request: IDBOpenDBRequest = indexedDB.open(DATABASE_NAME, DATABASE_VERSION);
    request.onupgradeneeded = (): void => {
      const database: IDBDatabase = request.result;
      if (!database.objectStoreNames.contains(WORKSPACE_STORE)) {
        const store: IDBObjectStore = database.createObjectStore(WORKSPACE_STORE, { keyPath: "key" });
        store.createIndex("ownerId", "ownerId", { unique: false });
      }
      if (!database.objectStoreNames.contains(SELECTION_STORE)) {
        database.createObjectStore(SELECTION_STORE, { keyPath: "ownerId" });
      }
    };
    request.onsuccess = (): void => resolve(request.result);
    request.onerror = (): void => reject(request.error ?? new Error("The browser could not open local workspace storage."));
  });
}

function parseWorkspaceRecords(values: unknown[]): StoredWorkspaceRecord[] {
  return values.flatMap((value: unknown): StoredWorkspaceRecord[] => {
    const parsed = storedWorkspaceRecordSchema.safeParse(value);
    return parsed.success ? [parsed.data] : [];
  });
}

export function loadWorkspaceRegistry(ownerId: string): Promise<WorkspaceRegistry> {
  return openWorkspaceDatabase().then((database: IDBDatabase): Promise<WorkspaceRegistry> => {
    const transaction: IDBTransaction = database.transaction([WORKSPACE_STORE, SELECTION_STORE], "readonly");
    const recordsRequest: IDBRequest<unknown[]> = transaction.objectStore(WORKSPACE_STORE).index("ownerId").getAll(ownerId);
    const selectionRequest: IDBRequest<unknown> = transaction.objectStore(SELECTION_STORE).get(ownerId);

    return Promise.all([requestResult(recordsRequest), requestResult(selectionRequest)])
      .then(([values, selectionValue]: [unknown[], unknown]): WorkspaceRegistry => {
        const records: StoredWorkspaceRecord[] = parseWorkspaceRecords(values)
          .filter((record: StoredWorkspaceRecord): boolean => record.ownerId === ownerId && record.workspace.ownerId === ownerId)
          .sort((left: StoredWorkspaceRecord, right: StoredWorkspaceRecord): number => right.workspace.addedAt.localeCompare(left.workspace.addedAt));
        const parsedSelection = storedWorkspaceSelectionSchema.safeParse(selectionValue);
        const selectedId: string | null = parsedSelection.success && parsedSelection.data.ownerId === ownerId
          ? parsedSelection.data.activeWorkspaceId
          : null;
        const activeWorkspaceId: string | null = selectedId && records.some((record: StoredWorkspaceRecord): boolean => record.workspace.id === selectedId)
          ? selectedId
          : null;
        return { workspaces: records.map((record: StoredWorkspaceRecord): ProjectWorkspace => record.workspace), activeWorkspaceId };
      })
      .finally((): void => database.close());
  });
}

export function listWorkspaceRecords(ownerId: string): Promise<StoredWorkspaceRecord[]> {
  return openWorkspaceDatabase().then((database: IDBDatabase): Promise<StoredWorkspaceRecord[]> => {
    const transaction: IDBTransaction = database.transaction(WORKSPACE_STORE, "readonly");
    const request: IDBRequest<unknown[]> = transaction.objectStore(WORKSPACE_STORE).index("ownerId").getAll(ownerId);
    return requestResult(request)
      .then((values: unknown[]): StoredWorkspaceRecord[] => parseWorkspaceRecords(values)
        .filter((record: StoredWorkspaceRecord): boolean => record.ownerId === ownerId && record.workspace.ownerId === ownerId))
      .finally((): void => database.close());
  });
}

export function getWorkspaceRecord(ownerId: string, workspaceId: string): Promise<StoredWorkspaceRecord | null> {
  return openWorkspaceDatabase().then((database: IDBDatabase): Promise<StoredWorkspaceRecord | null> => {
    const transaction: IDBTransaction = database.transaction(WORKSPACE_STORE, "readonly");
    const request: IDBRequest<unknown> = transaction.objectStore(WORKSPACE_STORE).get(workspaceKey(ownerId, workspaceId));
    return requestResult(request)
      .then((value: unknown): StoredWorkspaceRecord | null => {
        const parsed = storedWorkspaceRecordSchema.safeParse(value);
        return parsed.success && parsed.data.ownerId === ownerId && parsed.data.workspace.ownerId === ownerId
          ? parsed.data
          : null;
      })
      .finally((): void => database.close());
  });
}

export function saveWorkspaceRecord(record: StoredWorkspaceRecord): Promise<void> {
  const safeRecord: StoredWorkspaceRecord = storedWorkspaceRecordSchema.parse(record);
  return openWorkspaceDatabase().then((database: IDBDatabase): Promise<void> => {
    const transaction: IDBTransaction = database.transaction(WORKSPACE_STORE, "readwrite");
    transaction.objectStore(WORKSPACE_STORE).put(safeRecord);
    return transactionComplete(transaction).finally((): void => database.close());
  });
}

export function saveActiveWorkspaceId(ownerId: string, activeWorkspaceId: string | null): Promise<void> {
  const selection: StoredWorkspaceSelection = storedWorkspaceSelectionSchema.parse({ ownerId, activeWorkspaceId });
  return openWorkspaceDatabase().then((database: IDBDatabase): Promise<void> => {
    const transaction: IDBTransaction = database.transaction(SELECTION_STORE, "readwrite");
    transaction.objectStore(SELECTION_STORE).put(selection);
    return transactionComplete(transaction).finally((): void => database.close());
  });
}

export function savePendingWorkspaceSnapshot(
  record: StoredWorkspaceRecord,
  pendingSnapshot: WorkspaceSnapshot | null,
  snapshot: WorkspaceSnapshot = record.snapshot
): Promise<void> {
  return saveWorkspaceRecord({ ...record, snapshot, pendingSnapshot });
}

export function acceptPendingWorkspaceSnapshot(ownerId: string, workspaceId: string): Promise<void> {
  return getWorkspaceRecord(ownerId, workspaceId).then((record: StoredWorkspaceRecord | null): Promise<void> => {
    if (!record?.pendingSnapshot) return Promise.resolve();
    return saveWorkspaceRecord({ ...record, snapshot: record.pendingSnapshot, pendingSnapshot: null });
  });
}

export function deleteWorkspaceRecord(ownerId: string, workspaceId: string): Promise<void> {
  return openWorkspaceDatabase().then((database: IDBDatabase): Promise<void> => {
    const transaction: IDBTransaction = database.transaction(WORKSPACE_STORE, "readwrite");
    transaction.objectStore(WORKSPACE_STORE).delete(workspaceKey(ownerId, workspaceId));
    return transactionComplete(transaction).finally((): void => database.close());
  });
}
