import { Brain, ChatCircleDots, Coins, Cpu, ShieldCheck, Sparkle, SlidersHorizontal } from "@phosphor-icons/react";
import { useEffect, useRef, useState, type ReactElement } from "react";
import { Button } from "../../../../../shared/components/Button";
import type {
  AgentState,
  ConversationMessage,
  ConversationMode,
  StreamPhase,
  ToolExecution
} from "../../types/chatTypes";
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
  RunTimer,
  StatusLive,
  StreamError,
  ThinkingIndicator,
  ThinkingLogo,
  ThinkingDots,
  TokenSummary
} from "./styles";

type MessageListProps = {
  messages: ConversationMessage[];
  isLoading: boolean;
  hasConversation: boolean;
  hasModel: boolean;
  hasConfiguredProvider: boolean;
  phase: StreamPhase;
  startedAt: number | null;
  thinkingContent: string;
  streamingContent: string;
  errorMessage: string | null;
  agentState?: AgentState | null;
  toolExecutions?: ToolExecution[];
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

const formatElapsedTime = (elapsedSeconds: number): string => {
  const hours: number = Math.floor(elapsedSeconds / 3_600);
  const minutes: number = Math.floor((elapsedSeconds % 3_600) / 60);
  const seconds: number = elapsedSeconds % 60;
  const minuteSeconds: string = `${minutes.toString().padStart(2, "0")}:${seconds.toString().padStart(2, "0")}`;
  return hours > 0 ? `${hours.toString().padStart(2, "0")}:${minuteSeconds}` : minuteSeconds;
};

function GenerationStatus({ phase, startedAt }: { phase: StreamPhase; startedAt: number | null }): ReactElement {
  const fallbackStartedAt = useRef<number>(Date.now());
  const [now, setNow] = useState<number>(Date.now());

  useEffect((): (() => void) => {
    setNow(Date.now());
    const timer = window.setInterval((): void => setNow(Date.now()), 1_000);
    return (): void => window.clearInterval(timer);
  }, []);

  const elapsedSeconds: number = Math.max(0, Math.floor((now - (startedAt ?? fallbackStartedAt.current)) / 1_000));
  const label: string = phase === "starting"
    ? "Nexo is preparing a response"
    : phase === "cancelling" ? "Nexo is stopping the response" : "Nexo is responding";

  return (
    <RunStatus aria-label={label} title={label}>
      <ThinkingIndicator aria-hidden>
        <ThinkingLogo src="/assets/logo/nexo-ia-symbol.png" alt="" />
        <ThinkingDots><i /><i /><i /></ThinkingDots>
      </ThinkingIndicator>
      <RunTimer role="timer" aria-live="off" aria-label="Response generation elapsed time">
        {formatElapsedTime(elapsedSeconds)}
      </RunTimer>
    </RunStatus>
  );
}

export function MessageList({
  messages,
  isLoading,
  hasConversation,
  hasModel,
  hasConfiguredProvider,
  phase,
  startedAt,
  thinkingContent,
  streamingContent,
  errorMessage,
  agentState = null,
  toolExecutions = [],
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
  const conversationTokenTotal: number = messages.reduce((total: number, message: ConversationMessage): number =>
    total + (message.role === "ASSISTANT" ? message.totalTokens ?? 0 : 0), 0);
  const showRunStatus: boolean = phase === "starting" || phase === "streaming" || phase === "cancelling";

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

      {messages.map((message: ConversationMessage) => (
        <MessageItem
          key={message.id}
          message={message}
          streamingContent={streamingContent}
          thinkingContent={thinkingContent}
          isStreaming={message.id === streamingId}
          activeAgentState={message.id === streamingId ? agentState : null}
          activeToolExecutions={message.id === streamingId ? toolExecutions : []}
        />
      ))}

      {showRunStatus && <GenerationStatus phase={phase} startedAt={startedAt} />}

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
