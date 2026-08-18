import { useCallback, useEffect, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { ApiError } from "../../../../shared/api/ApiError";
import { cancelModelRequest } from "../api/chatApi";
import { streamMessage } from "../api/chatStreamClient";
import { messagesKey } from "./useChat";
import type {
  CancelledEvent,
  CompletedEvent,
  StartedEvent,
  StreamErrorEvent,
  StreamPhase,
  TokenEvent,
  UsageEvent
} from "../types/chatTypes";

export type ChatStream = {
  phase: StreamPhase;
  streamingContent: string;
  usage: UsageEvent | null;
  errorMessage: string | null;
  isBusy: boolean;
  send: (content: string) => void;
  cancel: () => void;
};

/**
 * Owns the lifecycle of one model request.
 *
 * <p>The text arriving from the stream stays in local state: copying a partial answer into the query
 * cache would duplicate the message once the persisted history is refetched. When the request
 * reaches a terminal state the server becomes the source of truth again.
 */
export const useChatStream = (conversationId: string | null): ChatStream => {
  const queryClient = useQueryClient();
  const [phase, setPhase] = useState<StreamPhase>("idle");
  const [streamingContent, setStreamingContent] = useState<string>("");
  const [usage, setUsage] = useState<UsageEvent | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const assistantMessageId = useRef<string | null>(null);
  const abortController = useRef<AbortController | null>(null);

  const settle = useCallback((next: StreamPhase): void => {
    setPhase(next);
    setStreamingContent("");
    assistantMessageId.current = null;
    abortController.current = null;
    queryClient.invalidateQueries({ queryKey: messagesKey(conversationId) });
  }, [conversationId, queryClient]);

  useEffect((): (() => void) => (): void => abortController.current?.abort(), [conversationId]);

  const send = useCallback((content: string): void => {
    if (!conversationId || phase === "starting" || phase === "streaming") return;

    const controller = new AbortController();
    abortController.current = controller;
    setPhase("starting");
    setStreamingContent("");
    setUsage(null);
    setErrorMessage(null);

    streamMessage(conversationId, content, {
      onStarted: (event: StartedEvent): void => {
        assistantMessageId.current = event.assistantMessageId;
        setPhase("streaming");
        queryClient.invalidateQueries({ queryKey: messagesKey(conversationId) });
      },
      onToken: (event: TokenEvent): void =>
        setStreamingContent((current: string): string => current + event.content),
      onUsage: (event: UsageEvent): void => setUsage(event),
      onCompleted: (_event: CompletedEvent): void => settle("completed"),
      onCancelled: (_event: CancelledEvent): void => settle("cancelled"),
      onError: (event: StreamErrorEvent): void => {
        setErrorMessage(event.message);
        settle("failed");
      }
    }, controller.signal)
      .catch((error: unknown): void => {
        if (controller.signal.aborted) return;
        setErrorMessage(error instanceof ApiError
          ? error.message
          : "The connection to Nexo IA was lost before the answer finished");
        settle(error instanceof ApiError ? "failed" : "disconnected");
      });
  }, [conversationId, phase, queryClient, settle]);

  const cancel = useCallback((): void => {
    if (!conversationId || !assistantMessageId.current) return;

    setPhase("cancelling");
    cancelModelRequest(conversationId, assistantMessageId.current)
      .catch((): void => undefined);
  }, [conversationId]);

  return {
    phase,
    streamingContent,
    usage,
    errorMessage,
    isBusy: phase === "starting" || phase === "streaming" || phase === "cancelling",
    send,
    cancel
  };
};
