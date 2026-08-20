import { create } from "zustand";
import type { ChatDraftState } from "../types/chatDraftTypes";

export const useChatDraftStore = create<ChatDraftState>((set) => ({
  content: "",
  setContent: (content: string): void => set({ content }),
  clear: (): void => set({ content: "" })
}));
