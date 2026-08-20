import { describe, expect, it } from "vitest";
import { workspaceSnapshotSchema } from "../schemas/workspaceSnapshotSchema";
import type { WorkspaceSnapshot } from "../types/workspaceSnapshotTypes";
import { captureWorkspaceSnapshot, compareWorkspaceSnapshots } from "./workspaceSnapshotService";

function fileHandle(name: string, content: string, lastModified: number): FileSystemFileHandle {
  return {
    kind: "file",
    name,
    getFile: () => Promise.resolve(new File([content], name, { lastModified }))
  } as unknown as FileSystemFileHandle;
}

function directoryHandle(
  name: string,
  children: Array<[string, FileSystemDirectoryHandle | FileSystemFileHandle]>
): FileSystemDirectoryHandle {
  return {
    kind: "directory",
    name,
    entries: async function* entries() {
      for (const child of children) yield child;
    }
  } as unknown as FileSystemDirectoryHandle;
}

describe("workspaceSnapshotService", () => {
  it("keeps existing saved snapshots readable before scan diagnostics were introduced", () => {
    expect(workspaceSnapshotSchema.safeParse({
      capturedAt: "2026-08-20T00:00:00.000Z",
      entries: [],
      truncated: false
    }).success).toBe(true);
  });

  it("captures nested project metadata without reading generated dependency trees", async () => {
    const root = directoryHandle("nexo", [
      ["src", directoryHandle("src", [["App.tsx", fileHandle("App.tsx", "export {}", 10)]])],
      ["node_modules", directoryHandle("node_modules", [["secret.js", fileHandle("secret.js", "large", 20)]])]
    ]);

    const snapshot: WorkspaceSnapshot = await captureWorkspaceSnapshot(root);

    expect(snapshot.entries.map((entry) => entry.path)).toEqual(["node_modules", "src", "src/App.tsx"]);
    expect(snapshot.entries.find((entry) => entry.path === "src/App.tsx")?.lastModified).toBe(10);
    expect(snapshot.truncated).toBe(true);
    expect(snapshot.scan).toMatchObject({
      maxEntries: 20_000,
      maxDepth: 32,
      omissionCount: 1,
      omissions: [{ path: "node_modules", reason: "ignored-directory" }]
    });
  });

  it("reports added, removed and modified project entries", () => {
    const previous: WorkspaceSnapshot = {
      capturedAt: "2026-08-20T00:00:00.000Z",
      truncated: false,
      entries: [
        { path: "README.md", kind: "file", size: 4, lastModified: 10 },
        { path: "old.ts", kind: "file", size: 3, lastModified: 10 }
      ]
    };
    const current: WorkspaceSnapshot = {
      capturedAt: "2026-08-20T01:00:00.000Z",
      truncated: false,
      entries: [
        { path: "README.md", kind: "file", size: 8, lastModified: 20 },
        { path: "new.ts", kind: "file", size: 3, lastModified: 10 }
      ]
    };

    expect(compareWorkspaceSnapshots(previous, current)).toEqual({
      added: ["new.ts"],
      removed: ["old.ts"],
      modified: ["README.md"],
      truncated: false
    });
  });
});
