import { Brain, ChatCircleDots, Coins, Cpu, ShieldCheck, Sparkle, SlidersHorizontal, SpinnerGap } from "@phosphor-icons/react";
import { useEffect, useRef, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import type { ConversationMessage, ConversationMode, StreamPhase } from "../../types/chatTypes";
import { ChatLoading } from "../ChatLoading";
import { MessageItem } from "./components/MessageItem";
import {
  Empty,
  EmptyIcon,
  EmptyKicker,
  EmptyTitle,
  Feature,
  FeatureGrid,
  Messages,
  RunStatus,
  StatusLive,
  StreamError,
  ThinkingTrace,
  TokenSummary
} from "./styles";

type MessageListProps = {
  messages: ConversationMessage[];
  isLoading: boolean;
  hasConversation: boolean;
  hasModel: boolean;
  hasConfiguredProvider: boolean;
  phase: StreamPhase;
  thinkingContent: string;
  streamingContent: string;
  errorMessage: string | null;
  accountTokenTotal?: number | null;
  mode: ConversationMode;
  onConfigureProvider: () => void;
};

/** Announces the real request lifecycle without presenting provider work as model reasoning. */
const streamStatus = (phase: StreamPhase): string => {
  if (phase === "starting") return "Request sent. Waiting for model output.";
  if (phase === "streaming") return "Nexo is responding.";
  if (phase === "cancelling") return "Stopping the response.";
  if (phase === "cancelled") return "Response stopped.";
  if (phase === "completed") return "Response complete.";
  if (phase === "failed") return "The response failed.";
  if (phase === "disconnected") return "The connection dropped.";
  return "";
};

export function MessageList({
  messages,
  isLoading,
  hasConversation,
  hasModel,
  hasConfiguredProvider,
  phase,
  thinkingContent,
  streamingContent,
  errorMessage,
  accountTokenTotal = null,
  mode,
  onConfigureProvider
}: MessageListProps): ReactElement {
  const bottom = useRef<HTMLDivElement>(null);

  useEffect((): void => {
    bottom.current?.scrollIntoView?.({ behavior: "smooth", block: "end" });
  }, [messages, streamingContent, thinkingContent]);

  if (!hasConversation) {
    return (
      <Messages>
        <Empty>
          <EmptyTitle>No conversation open</EmptyTitle>
          Start a new chat to talk to a model running on your own machine.
        </Empty>
      </Messages>
    );
  }

  if (isLoading) return <ChatLoading title="Opening conversation" label="Nexo is restoring this private thread…" />;

  const streamingId: string | null = phase === "streaming" || phase === "cancelling"
    ? messages.findLast((message: ConversationMessage) => message.role === "ASSISTANT")?.id ?? null
    : null;
  const waitingForFirstToken: boolean = Boolean(streamingId) && !streamingContent;
  const conversationTokenTotal: number = messages.reduce((total: number, message: ConversationMessage): number =>
    total + (message.role === "ASSISTANT" ? message.totalTokens ?? 0 : 0), 0);
  const showRunStatus: boolean = phase === "starting" || waitingForFirstToken;

  return (
    <Messages>
      {streamStatus(phase) && (
        <StatusLive role="status" aria-live="polite">{streamStatus(phase)}</StatusLive>
      )}

      {(conversationTokenTotal > 0 || accountTokenTotal !== null) && (
        <TokenSummary aria-label="Token usage summary">
          <Coins size={14} weight="duotone" />
          <span>Conversation <strong>{conversationTokenTotal.toLocaleString()}</strong> tokens</span>
          {accountTokenTotal !== null && <span>Account <strong>{accountTokenTotal.toLocaleString()}</strong> total</span>}
        </TokenSummary>
      )}

      {messages.length === 0 && phase === "idle" && (
        <Empty>
          <EmptyIcon $agent={mode === "agent"}>{mode === "agent" ? <Brain size={28} weight="duotone" /> : <Sparkle size={28} weight="duotone" />}</EmptyIcon>
          <EmptyKicker>{mode === "agent" ? "Agent workspace" : "Nexo intelligence"}</EmptyKicker>
          <EmptyTitle>{hasModel
            ? mode === "agent" ? "Turn an objective into verified work" : "What are we exploring today?"
            : hasConfiguredProvider ? "Choose a model to begin" : "Set up a model to begin"}</EmptyTitle>
          <span>{hasModel
            ? mode === "agent" ? "Your conversation stays here while Nexo prepares plans, permissions and evidence." : "Ask, create, analyse or bring an idea. Tools stay available when the task needs them."
            : hasConfiguredProvider
              ? "Pick one of your configured local models in the conversation header."
              : "You have no provider configured yet. Add a local Ollama model to start talking to Nexo."}</span>
          {!hasModel && !hasConfiguredProvider && (
            <Button type="button" icon={SlidersHorizontal} onClick={onConfigureProvider}>
              Configure a provider
            </Button>
          )}
          {hasModel && <FeatureGrid>
            <Feature><ChatCircleDots size={17} weight="duotone" /><strong>Natural conversation</strong><small>Context stays inside this private thread.</small></Feature>
            <Feature><Cpu size={17} weight="duotone" /><strong>Local model</strong><small>Your selected provider handles the response.</small></Feature>
            <Feature><ShieldCheck size={17} weight="duotone" /><strong>Governed action</strong><small>Capabilities remain visible and controlled.</small></Feature>
          </FeatureGrid>}
        </Empty>
      )}

      {streamingId && thinkingContent && (
        <ThinkingTrace open>
          <summary><Brain size={15} weight="duotone" /> Thinking <small>live · not saved</small></summary>
          <p>{thinkingContent}</p>
        </ThinkingTrace>
      )}

      {showRunStatus && (
        <RunStatus role="status">
          <SpinnerGap size={18} weight="bold" />
          <span><strong>Nexo is working in this conversation</strong><small>You can open another chat and come back without stopping it.</small></span>
        </RunStatus>
      )}

      {messages.map((message: ConversationMessage) => message.id === streamingId && waitingForFirstToken
        ? null
        : (
          <MessageItem
            key={message.id}
            message={message}
            streamingContent={streamingContent}
            isStreaming={message.id === streamingId}
          />
        ))}

      {errorMessage && <StreamError role="alert">{errorMessage}</StreamError>}
      {phase === "disconnected" && (
        <StreamError role="alert">
          The connection dropped. Reopen this conversation to see what was saved.
        </StreamError>
      )}

      <div ref={bottom} />
    </Messages>
  );
}
