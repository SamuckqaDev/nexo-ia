import type {
  WorkspaceSnapshot,
  WorkspaceSnapshotDifference,
  WorkspaceSnapshotEntry,
  WorkspaceSnapshotOmission,
  WorkspaceSnapshotOmissionReason
} from "../types/workspaceSnapshotTypes";

const MAX_SNAPSHOT_ENTRIES = 20_000;
const MAX_SNAPSHOT_DEPTH = 32;
const MAX_REPORTED_OMISSIONS = 100;
const ignoredDirectoryNames: ReadonlySet<string> = new Set([
  ".git",
  ".gradle",
  ".idea",
  ".next",
  ".turbo",
  ".vite",
  "build",
  "coverage",
  "dist",
  "node_modules",
  "target"
]);

type SnapshotAccumulator = {
  entries: WorkspaceSnapshotEntry[];
  truncated: boolean;
  omissionCount: number;
  omissions: WorkspaceSnapshotOmission[];
};

function recordOmission(
  accumulator: SnapshotAccumulator,
  path: string,
  reason: WorkspaceSnapshotOmissionReason
): void {
  accumulator.truncated = true;
  accumulator.omissionCount += 1;
  if (accumulator.omissions.length < MAX_REPORTED_OMISSIONS) {
    accumulator.omissions.push({ path, reason });
  }
}

async function collectDirectory(
  directory: FileSystemDirectoryHandle,
  prefix: string,
  depth: number,
  accumulator: SnapshotAccumulator
): Promise<void> {
  if (depth > MAX_SNAPSHOT_DEPTH) {
    recordOmission(accumulator, prefix || directory.name, "depth-limit");
    return;
  }

  try {
    for await (const [name, handle] of directory.entries()) {
      if (accumulator.entries.length >= MAX_SNAPSHOT_ENTRIES) {
        recordOmission(accumulator, prefix || directory.name, "entry-limit");
        return;
      }

      const path: string = prefix ? `${prefix}/${name}` : name;

      if (handle.kind === "directory") {
        accumulator.entries.push({ path, kind: "directory", size: null, lastModified: null });
        if (ignoredDirectoryNames.has(name)) {
          recordOmission(accumulator, path, "ignored-directory");
        } else {
          await collectDirectory(handle, path, depth + 1, accumulator);
        }
        continue;
      }

      try {
        const file: File = await handle.getFile();
        accumulator.entries.push({
          path,
          kind: "file",
          size: file.size,
          lastModified: file.lastModified
        });
      } catch {
        recordOmission(accumulator, path, "unreadable-entry");
      }
    }
  } catch {
    recordOmission(accumulator, prefix || directory.name, "unreadable-entry");
  }
}

export async function captureWorkspaceSnapshot(directory: FileSystemDirectoryHandle): Promise<WorkspaceSnapshot> {
  const accumulator: SnapshotAccumulator = {
    entries: [],
    truncated: false,
    omissionCount: 0,
    omissions: []
  };
  await collectDirectory(directory, "", 0, accumulator);
  accumulator.entries.sort((left: WorkspaceSnapshotEntry, right: WorkspaceSnapshotEntry): number => left.path.localeCompare(right.path));
  return {
    capturedAt: new Date().toISOString(),
    entries: accumulator.entries,
    truncated: accumulator.truncated,
    scan: {
      maxEntries: MAX_SNAPSHOT_ENTRIES,
      maxDepth: MAX_SNAPSHOT_DEPTH,
      omissionCount: accumulator.omissionCount,
      omissions: accumulator.omissions
    }
  };
}

function entryChanged(previous: WorkspaceSnapshotEntry, current: WorkspaceSnapshotEntry): boolean {
  return previous.kind !== current.kind
    || previous.size !== current.size
    || previous.lastModified !== current.lastModified;
}

export function compareWorkspaceSnapshots(
  previous: WorkspaceSnapshot,
  current: WorkspaceSnapshot
): WorkspaceSnapshotDifference {
  const previousEntries: Map<string, WorkspaceSnapshotEntry> = new Map(
    previous.entries.map((entry: WorkspaceSnapshotEntry): [string, WorkspaceSnapshotEntry] => [entry.path, entry])
  );
  const currentEntries: Map<string, WorkspaceSnapshotEntry> = new Map(
    current.entries.map((entry: WorkspaceSnapshotEntry): [string, WorkspaceSnapshotEntry] => [entry.path, entry])
  );
  const added: string[] = [];
  const removed: string[] = [];
  const modified: string[] = [];

  currentEntries.forEach((entry: WorkspaceSnapshotEntry, path: string): void => {
    const previousEntry: WorkspaceSnapshotEntry | undefined = previousEntries.get(path);
    if (!previousEntry) added.push(path);
    else if (entryChanged(previousEntry, entry)) modified.push(path);
  });
  previousEntries.forEach((_entry: WorkspaceSnapshotEntry, path: string): void => {
    if (!currentEntries.has(path)) removed.push(path);
  });

  return {
    added: added.sort(),
    removed: removed.sort(),
    modified: modified.sort(),
    truncated: previous.truncated || current.truncated
  };
}
