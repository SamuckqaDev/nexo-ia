import type { z } from "zod";
import type {
  conversationMessageSchema,
  conversationSchema,
  agentPlanSchema,
  agentPlanStepStatusSchema,
  agentStateSchema,
  messageStatusSchema,
  processingLocationSchema,
  tokenSourceSchema,
  toolExecutionSchema
} from "../schemas/chatSchemas";
import type {
  cancelledEventSchema,
  agentStateEventSchema,
  completedEventSchema,
  startedEventSchema,
  streamErrorEventSchema,
  streamEventSchema,
  thinkingEventSchema,
  toolCompletedEventSchema,
  planUpdatedEventSchema,
  toolStartedEventSchema,
  tokenEventSchema,
  usageEventSchema
} from "../schemas/streamEventSchemas";

export type Conversation = z.infer<typeof conversationSchema>;
export type ConversationMessage = z.infer<typeof conversationMessageSchema>;
export type MessageStatus = z.infer<typeof messageStatusSchema>;
export type TokenSource = z.infer<typeof tokenSourceSchema>;
export type ProcessingLocation = z.infer<typeof processingLocationSchema>;
export type AgentState = z.infer<typeof agentStateSchema>;
export type AgentPlan = z.infer<typeof agentPlanSchema>;
export type AgentPlanStepStatus = z.infer<typeof agentPlanStepStatusSchema>;
export type ToolExecution = z.infer<typeof toolExecutionSchema>;

export type StartedEvent = z.infer<typeof startedEventSchema>;
export type ThinkingEvent = z.infer<typeof thinkingEventSchema>;
export type AgentStateEvent = z.infer<typeof agentStateEventSchema>;
export type ToolStartedEvent = z.infer<typeof toolStartedEventSchema>;
export type ToolCompletedEvent = z.infer<typeof toolCompletedEventSchema>;
export type PlanUpdatedEvent = z.infer<typeof planUpdatedEventSchema>;
export type TokenEvent = z.infer<typeof tokenEventSchema>;
export type UsageEvent = z.infer<typeof usageEventSchema>;
export type CompletedEvent = z.infer<typeof completedEventSchema>;
export type CancelledEvent = z.infer<typeof cancelledEventSchema>;
export type StreamErrorEvent = z.infer<typeof streamErrorEventSchema>;
export type StreamEvent = z.infer<typeof streamEventSchema>;

/** Lifecycle of the local streaming surface, distinct from the persisted message status. */
export type StreamPhase =
  | "idle"
  | "starting"
  | "streaming"
  | "cancelling"
  | "cancelled"
  | "completed"
  | "failed"
  | "disconnected";

export type ChatStreamHandlers = {
  onStarted: (event: StartedEvent) => void;
  onThinking: (event: ThinkingEvent) => void;
  onAgentState: (event: AgentStateEvent) => void;
  onToolStarted: (event: ToolStartedEvent) => void;
  onToolCompleted: (event: ToolCompletedEvent) => void;
  onPlanUpdated: (event: PlanUpdatedEvent) => void;
  onToken: (event: TokenEvent) => void;
  onUsage: (event: UsageEvent) => void;
  onCompleted: (event: CompletedEvent) => void;
  onCancelled: (event: CancelledEvent) => void;
  onError: (event: StreamErrorEvent) => void;
};

export type ConversationMode = "chat" | "agent";
