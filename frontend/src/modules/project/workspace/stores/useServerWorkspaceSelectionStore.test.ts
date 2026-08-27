import { beforeEach, describe, expect, it } from "vitest";
import { useServerWorkspaceSelectionStore } from "./useServerWorkspaceSelectionStore";

describe("useServerWorkspaceSelectionStore", () => {
  beforeEach(() => {
    window.localStorage.clear();
    useServerWorkspaceSelectionStore.setState({ ownerId: null, selectedWorkspaceId: null });
  });

  it("persists the active server workspace separately for each signed-in user", () => {
    const store = useServerWorkspaceSelectionStore.getState();
    store.initialize("user-one");
    useServerWorkspaceSelectionStore.getState().selectWorkspace("workspace-one");

    useServerWorkspaceSelectionStore.getState().initialize("user-two");
    expect(useServerWorkspaceSelectionStore.getState().selectedWorkspaceId).toBeNull();

    useServerWorkspaceSelectionStore.getState().initialize("user-one");
    expect(useServerWorkspaceSelectionStore.getState().selectedWorkspaceId).toBe("workspace-one");
  });
});
