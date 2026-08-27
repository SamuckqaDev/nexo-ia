import { describe, expect, it } from "vitest";
import { WorkspaceSelectionStore } from "./workspaceSelectionStore.js";

describe("WorkspaceSelectionStore", () => {
  it("returns display metadata without exposing the native path", () => {
    const store = new WorkspaceSelectionStore({ idFactory: () => "selection-1" });

    expect(store.create("/Users/samuel/projects/nexo-ia")).toEqual({
      selectionId: "selection-1",
      displayName: "nexo-ia",
      existingWorkspaceId: null
    });
    expect(store.consume("selection-1")).toBe("/Users/samuel/projects/nexo-ia");
    expect(() => store.consume("selection-1")).toThrow("expired");
  });

  it("expires abandoned selections", () => {
    let now = 1_000;
    const store = new WorkspaceSelectionStore({
      clock: () => now,
      idFactory: () => "selection-2",
      ttlMs: 100
    });
    store.create("/home/samuel/projects/nexo-ia");
    now = 1_101;

    expect(() => store.consume("selection-2")).toThrow("expired");
  });
});
