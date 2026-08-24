import { beforeEach, describe, expect, it } from "vitest";
import { useConversationModeStore } from "./useConversationModeStore";

describe("useConversationModeStore", () => {
  beforeEach(() => {
    localStorage.clear();
    useConversationModeStore.setState({ mode: "chat" });
  });

  it("persists Agent mode while the user navigates away from Chat", () => {
    useConversationModeStore.getState().setMode("agent");

    expect(useConversationModeStore.getState().mode).toBe("agent");
    expect(JSON.parse(localStorage.getItem("nexo-conversation-mode") ?? "{}"))
      .toMatchObject({ state: { mode: "agent" } });
  });
});
