import { create } from "zustand";
import type { ChatStreamState, ConversationStreamSnapshot } from "../types/chatStreamTypes";

export const idleConversationStream: ConversationStreamSnapshot = {
  phase: "idle",
  startedAt: null,
  thinkingContent: "",
  streamingContent: "",
  usage: null,
  errorMessage: null,
  assistantMessageId: null
};

export const useChatStreamStore = create<ChatStreamState>((set) => ({
  streams: {},
  updateStream: (conversationId: string, update: Partial<ConversationStreamSnapshot>): void => {
    set((state: ChatStreamState): Pick<ChatStreamState, "streams"> => ({
      streams: {
        ...state.streams,
        [conversationId]: {
          ...(state.streams[conversationId] ?? idleConversationStream),
          ...update
        }
      }
    }));
  },
  reset: (): void => set({ streams: {} })
}));
