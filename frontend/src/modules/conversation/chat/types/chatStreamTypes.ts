import type { AgentState, StreamPhase, ToolExecution, UsageEvent } from "./chatTypes";

export type ConversationStreamSnapshot = {
  phase: StreamPhase;
  startedAt: number | null;
  thinkingContent: string;
  streamingContent: string;
  usage: UsageEvent | null;
  errorMessage: string | null;
  assistantMessageId: string | null;
  agentState: AgentState | null;
  toolExecutions: ToolExecution[];
};

export type ChatStreamState = {
  streams: Record<string, ConversationStreamSnapshot>;
  updateStream: (conversationId: string, update: Partial<ConversationStreamSnapshot>) => void;
  reset: () => void;
};
