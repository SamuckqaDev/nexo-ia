import type { StreamPhase, UsageEvent } from "./chatTypes";

export type ConversationStreamSnapshot = {
  phase: StreamPhase;
  startedAt: number | null;
  thinkingContent: string;
  streamingContent: string;
  usage: UsageEvent | null;
  errorMessage: string | null;
  assistantMessageId: string | null;
};

export type ChatStreamState = {
  streams: Record<string, ConversationStreamSnapshot>;
  updateStream: (conversationId: string, update: Partial<ConversationStreamSnapshot>) => void;
  reset: () => void;
};
