import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { ConversationMode } from "../types/chatTypes";

type ConversationModeState = {
  mode: ConversationMode;
  setMode: (mode: ConversationMode) => void;
};

/** Keeps the user's selected Chat/Agent surface stable while navigating to MCP, Vaults, or settings. */
export const useConversationModeStore = create<ConversationModeState>()(persist((set) => ({
  mode: "chat",
  setMode: (mode): void => {
    set({ mode });
  }
}), { name: "nexo-conversation-mode" }));
