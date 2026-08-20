import { useCallback, useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../../../shared/api/ApiError";
import { usePreferenceStore } from "../../../settings/stores/usePreferenceStore";
import type { PreferenceState } from "../../../settings/types/preferenceTypes";
import { cancelModelRequest } from "../api/chatApi";
import { streamMessage } from "../api/chatStreamClient";
import { idleConversationStream, useChatStreamStore } from "../stores/useChatStreamStore";
import type { ChatStreamState, ConversationStreamSnapshot } from "../types/chatStreamTypes";
import type {
  CancelledEvent,
  CompletedEvent,
  ConversationMessage,
  StartedEvent,
  StreamErrorEvent,
  StreamPhase,
  ThinkingEvent,
  TokenEvent,
  UsageEvent
} from "../types/chatTypes";
import { conversationsKey, messagesKey } from "./useChat";

export type ChatStream = {
  phase: StreamPhase;
  thinkingContent: string;
  streamingContent: string;
  usage: UsageEvent | null;
  errorMessage: string | null;
  isBusy: boolean;
  send: (content: string) => void;
  cancel: () => void;
};

const activeRequests = new Map<string, AbortController>();
const activeStatuses = new Set(["QUEUED", "STREAMING", "CANCELLING"]);

const persistedPhase = (message: ConversationMessage): StreamPhase => {
  if (message.status === "QUEUED") return "starting";
  if (message.status === "STREAMING") return "streaming";
  if (message.status === "CANCELLING") return "cancelling";
  if (message.status === "CANCELLED") return "cancelled";
  if (message.status === "FAILED") return "failed";
  return "completed";
};

export const resetChatStreams = (): void => {
  activeRequests.forEach((controller: AbortController): void => controller.abort());
  activeRequests.clear();
  useChatStreamStore.getState().reset();
};

export const cancelAllChatStreams = (): Promise<void> => {
  const streams: Record<string, ConversationStreamSnapshot> = useChatStreamStore.getState().streams;
  const cancellations: Promise<void>[] = Object.entries(streams)
    .filter(([, stream]: [string, ConversationStreamSnapshot]): boolean => Boolean(stream.assistantMessageId)
      && (stream.phase === "starting" || stream.phase === "streaming" || stream.phase === "cancelling"))
    .map(([conversationId, stream]: [string, ConversationStreamSnapshot]): Promise<void> =>
      cancelModelRequest(conversationId, stream.assistantMessageId ?? "").catch((): void => undefined));
  return Promise.all(cancellations).then((): void => resetChatStreams());
};

/**
 * Owns model streams independently from the selected conversation.
 *
 * Navigating between threads only changes the subscribed snapshot. The originating request remains
 * connected and keeps updating its own conversation until it reaches a terminal state. A request
 * restored from persistence is polled until the backend reports its terminal message.
 */
export const useChatStream = (
  conversationId: string | null,
  persistedMessages: ConversationMessage[] = []
): ChatStream => {
  const queryClient = useQueryClient();
  const thinkingEnabled: boolean = usePreferenceStore((state: PreferenceState) => state.thinkingEnabled);
  const snapshot: ConversationStreamSnapshot = useChatStreamStore((state: ChatStreamState) =>
    conversationId ? state.streams[conversationId] ?? idleConversationStream : idleConversationStream);
  const updateStream: ChatStreamState["updateStream"] = useChatStreamStore(
    (state: ChatStreamState) => state.updateStream);

  const latestAssistant: ConversationMessage | undefined = persistedMessages
    .findLast((message: ConversationMessage): boolean => message.role === "ASSISTANT");

  useEffect((): (() => void) | void => {
    if (!conversationId || activeRequests.has(conversationId)) return;

    if (latestAssistant && activeStatuses.has(latestAssistant.status)) {
      updateStream(conversationId, {
        phase: persistedPhase(latestAssistant),
        assistantMessageId: latestAssistant.id,
        errorMessage: null
      });
      const refresh = window.setInterval((): void => {
        queryClient.invalidateQueries({ queryKey: messagesKey(conversationId) });
        queryClient.invalidateQueries({ queryKey: conversationsKey });
      }, 1_200);
      return (): void => window.clearInterval(refresh);
    }

    const stored: ConversationStreamSnapshot | undefined = useChatStreamStore.getState().streams[conversationId];
    if (stored && (stored.phase === "starting" || stored.phase === "streaming" || stored.phase === "cancelling")) {
      updateStream(conversationId, {
        phase: latestAssistant ? persistedPhase(latestAssistant) : "idle",
        thinkingContent: "",
        streamingContent: "",
        assistantMessageId: null
      });
    }
  }, [conversationId, latestAssistant, queryClient, updateStream]);

  const settle = useCallback((targetConversationId: string, next: StreamPhase): void => {
    activeRequests.delete(targetConversationId);
    updateStream(targetConversationId, {
      phase: next,
      thinkingContent: "",
      streamingContent: "",
      assistantMessageId: null
    });
    queryClient.invalidateQueries({ queryKey: messagesKey(targetConversationId) });
    queryClient.invalidateQueries({ queryKey: conversationsKey });
    queryClient.invalidateQueries({ queryKey: ["usage"] });
  }, [queryClient, updateStream]);

  const send = useCallback((content: string): void => {
    if (!conversationId || activeRequests.has(conversationId)) return;

    const controller = new AbortController();
    activeRequests.set(conversationId, controller);
    updateStream(conversationId, {
      phase: "starting",
      thinkingContent: "",
      streamingContent: "",
      usage: null,
      errorMessage: null,
      assistantMessageId: null
    });

    streamMessage(conversationId, content, thinkingEnabled, {
      onStarted: (event: StartedEvent): void => {
        updateStream(conversationId, { phase: "streaming", assistantMessageId: event.assistantMessageId });
        queryClient.invalidateQueries({ queryKey: messagesKey(conversationId) });
      },
      onThinking: (event: ThinkingEvent): void => {
        const current: ConversationStreamSnapshot = useChatStreamStore.getState().streams[conversationId]
          ?? idleConversationStream;
        updateStream(conversationId, { thinkingContent: current.thinkingContent + event.content });
      },
      onToken: (event: TokenEvent): void => {
        const current: ConversationStreamSnapshot = useChatStreamStore.getState().streams[conversationId]
          ?? idleConversationStream;
        updateStream(conversationId, { streamingContent: current.streamingContent + event.content });
      },
      onUsage: (event: UsageEvent): void => updateStream(conversationId, { usage: event }),
      onCompleted: (_event: CompletedEvent): void => settle(conversationId, "completed"),
      onCancelled: (_event: CancelledEvent): void => settle(conversationId, "cancelled"),
      onError: (event: StreamErrorEvent): void => {
        updateStream(conversationId, { errorMessage: event.message });
        settle(conversationId, "failed");
      }
    }, controller.signal)
      .then((): void => {
        const current: ConversationStreamSnapshot | undefined = useChatStreamStore.getState().streams[conversationId];
        if (current && activeRequests.has(conversationId)
          && (current.phase === "starting" || current.phase === "streaming" || current.phase === "cancelling")) {
          updateStream(conversationId, {
            errorMessage: "The connection to Nexo IA closed before the final status arrived"
          });
          settle(conversationId, "disconnected");
        }
      })
      .catch((error: unknown): void => {
        if (controller.signal.aborted) return;
        updateStream(conversationId, {
          errorMessage: error instanceof ApiError
            ? error.message
            : "The connection to Nexo IA was lost before the answer finished"
        });
        settle(conversationId, error instanceof ApiError ? "failed" : "disconnected");
      });
  }, [conversationId, queryClient, settle, thinkingEnabled, updateStream]);

  const cancel = useCallback((): void => {
    if (!conversationId || !snapshot.assistantMessageId) return;

    updateStream(conversationId, { phase: "cancelling" });
    cancelModelRequest(conversationId, snapshot.assistantMessageId)
      .catch((): void => undefined);
  }, [conversationId, snapshot.assistantMessageId, updateStream]);

  return {
    phase: snapshot.phase,
    thinkingContent: snapshot.thinkingContent,
    streamingContent: snapshot.streamingContent,
    usage: snapshot.usage,
    errorMessage: snapshot.errorMessage,
    isBusy: snapshot.phase === "starting" || snapshot.phase === "streaming" || snapshot.phase === "cancelling",
    send,
    cancel
  };
};
